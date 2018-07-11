/*
 *  Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.util;

import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.config.MetricCharacterReplacer;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.util.PathResolver;
import com.appdynamics.extensions.logmonitor.config.SearchPattern;
import com.appdynamics.extensions.logmonitor.config.SearchString;
import com.appdynamics.extensions.logmonitor.processors.FilePointer;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import org.apache.commons.lang.StringUtils;
import org.bitbucket.kienerj.OptimizedRandomAccessFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * @author Florencio Sarmiento
 */
public class LogMonitorUtil {
    private static final Logger logger = LoggerFactory.getLogger(LogMonitorUtil.class);
    private static final String CASE_SENSITIVE_PATTERN = "(?-i)";
    private static final String CASE_INSENSITIVE_PATTERN = "(?i)";

    public static String resolvePath(String filename) {
        if (StringUtils.isBlank(filename)) {
            return "";
        }

        //for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }

        //for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = String.format("%s%s%s", jarPath, File.separator, filename);
        return configFileName;
    }

    public static List<SearchPattern> createPattern(List<SearchString> searchStrings) {
        List<SearchPattern> searchPatterns = new ArrayList<SearchPattern>();
        if (searchStrings != null && !searchStrings.isEmpty()) {

            for (SearchString searchString : searchStrings) {
                Pattern pattern;

                StringBuilder rawPatternsStringBuilder = new StringBuilder();

                if (searchString.getCaseSensitive()) {
                    rawPatternsStringBuilder.append(CASE_SENSITIVE_PATTERN);
                } else {
                    rawPatternsStringBuilder.append(CASE_INSENSITIVE_PATTERN);
                }

                if (searchString.getMatchExactString()) {

                    rawPatternsStringBuilder.append("(?<=\\s|^)");
                    rawPatternsStringBuilder.append(Pattern.quote(searchString.getPattern().trim()));
                    rawPatternsStringBuilder.append("(?=\\s|$)");

                } else {

                    rawPatternsStringBuilder.append(searchString.getPattern().trim());
                }

                pattern = Pattern.compile(rawPatternsStringBuilder.toString());

                SearchPattern searchPattern = new SearchPattern(searchString.getDisplayName(), pattern,
                        searchString.getCaseSensitive(), searchString.getPrintMatchedString(), searchString.getSendEventToController());
                searchPatterns.add(searchPattern);
            }
        }
        return searchPatterns;
    }

    public static void closeRandomAccessFile(OptimizedRandomAccessFile randomAccessFile) {
        if (randomAccessFile != null) {
            try {
                randomAccessFile.close();
            } catch (IOException e) {
                logger.debug("An error occurred while closing the random access file : " + e.getMessage());
            }
        }
    }

    public static BigInteger convertValueToZeroIfNullOrNegative(BigInteger value) {
        if (value == null || value.compareTo(BigInteger.ZERO) < 0) {
            return BigInteger.ZERO;
        }

        return value;
    }

    public static long getCurrentFileCreationTimeStamp(File file) throws IOException {
        Path p = Paths.get(file.getAbsolutePath());
        BasicFileAttributes view
                = Files.getFileAttributeView(p, BasicFileAttributeView.class)
                .readAttributes();
        return view.creationTime().toMillis();
    }

    public static FilePointer getLatestFilePointer(CopyOnWriteArrayList<FilePointer> filePointers) {
        return Collections.max(filePointers, new Comparator<FilePointer>() {
            public int compare(FilePointer file1, FilePointer file2) {
                if (file1.getFileCreationTime() > file2.getFileCreationTime())
                    return 1;
                else if (file1.getFileCreationTime() < file2.getFileCreationTime())
                    return -1;
                return 0;
            }
        });
    }

    public static List<Log> getValidLogsFromConfig(List<Map<String, ?>> logsFromConfig) {
        List<Log> validLogs = new ArrayList<Log>();
        for (Map<String, ?> logFromConfig : logsFromConfig) {
            try {
                Log log = initializeLog(logFromConfig);
                LogConfigValidator.validate(log);
                validLogs.add(log);
            } catch (IllegalArgumentException ex) {
                logger.error("Invalid Log Configuration : " + logFromConfig.get("displayName"), ex);
            }
        }
        return validLogs;
    }


    private static Log initializeLog(Map<String, ?> currentLogFromConfig) {
        Log log = new Log();
        log.setDisplayName((String) currentLogFromConfig.get("displayName"));
        log.setDisplayName((String) currentLogFromConfig.get("logName"));
        log.setDisplayName((String) currentLogFromConfig.get("logDirectory"));
        log.setSearchStrings(initializeSearchStrings(currentLogFromConfig));
        return log;
    }

    private static List<SearchString> initializeSearchStrings(Map<String, ?> currentLogFromConfig) {
        List<SearchString> searchStrings = Lists.newArrayList();
        List<Map<String, ?>> searchStringsForCurrentLog = (List) currentLogFromConfig.get("searchStrings");
        for(Map<String, ?> searchStringFromLog : searchStringsForCurrentLog) {
            SearchString searchString = new SearchString();
            searchString.setDisplayName((String) searchStringFromLog.get("displayName"));
            searchString.setPattern((String) searchStringFromLog.get("pattern"));
            searchString.setMatchExactString((Boolean) searchStringFromLog.get("matchExactString"));
            searchString.setCaseSensitive((Boolean) searchStringFromLog.get("caseSensitive"));
            searchString.setPrintMatchedString((Boolean) searchStringFromLog.get("printMatchedString"));
            searchStrings.add(searchString);
        }
        return searchStrings;
    }

    public static Map<Pattern, String> initializeMetricCharacterReplacers(List<Map<String, String>> metricCharacterReplacers) {
        Map<Pattern, String> replacers = new HashMap<Pattern, String>();
        if (metricCharacterReplacers != null) {
            for (Map<String, String> metricCharacterReplacer : metricCharacterReplacers) {
                String replace = metricCharacterReplacer.get("replace");
                String replaceWith = metricCharacterReplacer.get("replaceWith");
                Pattern pattern = Pattern.compile(replace);
                replacers.put(pattern, replaceWith);
            }
        }
        return replacers;
    }

    public static List<Metric> processRawMetrics(Map<String, BigInteger> rawMetrics) {
        List<Metric> allLogMetrics = Lists.newArrayList();
        return allLogMetrics;
    }
}
