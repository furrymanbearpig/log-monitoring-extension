/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.processors;

import com.appdynamics.extensions.logmonitor.config.FilePointer;
import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.config.SearchPattern;
import com.appdynamics.extensions.logmonitor.metrics.LogMetrics;
import com.appdynamics.extensions.metrics.Metric;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bitbucket.kienerj.OptimizedRandomAccessFile;

import java.io.File;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.appdynamics.extensions.logmonitor.LogMonitor.baseMetrics;
import static com.appdynamics.extensions.logmonitor.util.Constants.*;
import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.closeRandomAccessFile;
import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.createPattern;
import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.getCurrentFileCreationTimeStamp;

/**
 * Created by aditya.jagtiani on 6/18/18.
 */

public class LogMetricsProcessor implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogMetricsProcessor.class);
    private OptimizedRandomAccessFile randomAccessFile;
    private Log log;
    private CountDownLatch latch;
    private LogMetrics logMetrics;
    private File currentFile;
    private List<SearchPattern> searchPatterns;
    private Map<Pattern, String> replacers;

    LogMetricsProcessor(OptimizedRandomAccessFile randomAccessFile, Log log, CountDownLatch latch, LogMetrics logMetrics,
                        File currentFile, Map<Pattern, String> replacers) {
        this.randomAccessFile = randomAccessFile;
        this.log = log;
        this.latch = latch;
        this.logMetrics = logMetrics;
        this.currentFile = currentFile;
        this.replacers = replacers;
        this.searchPatterns = createPattern(this.log.getSearchStrings());
    }

    public void run() {
        try {
            processLogFile();
        } catch (Exception ex) {
            LOGGER.error("Error encountered while processing log file : {}", log.getDisplayName(), ex);
        } finally {
            closeRandomAccessFile(randomAccessFile);
            latch.countDown();
        }
    }

    private void processLogFile() throws Exception {
        long currentFilePointer = randomAccessFile.getFilePointer();
        String currentLine;
        while ((currentLine = randomAccessFile.readLine()) != null) {
            incrementWordCountIfSearchStringMatched(searchPatterns, currentLine, logMetrics);
            currentFilePointer = randomAccessFile.getFilePointer();
        }
        setBasePatternOccurrenceCount();
        long currentFileCreationTime = getCurrentFileCreationTimeStamp(currentFile);
        String metricName = getLogNamePrefix() + FILESIZE_METRIC_NAME;
        logMetrics.add(metricName, new Metric(metricName,
                String.valueOf(randomAccessFile.length()), logMetrics.getMetricPrefix() + METRIC_SEPARATOR
                + metricName));
        updateCurrentFilePointer(currentFile.getPath(), currentFilePointer, currentFileCreationTime);
        LOGGER.info(String.format("Successfully processed log file [%s]",
                randomAccessFile));
    }

    private void setBasePatternOccurrenceCount() {
        for (SearchPattern searchPattern : searchPatterns) {
            String logMetricPrefix = getSearchStringPrefix();
            String currentKey = logMetricPrefix + searchPattern.getDisplayName() + METRIC_SEPARATOR;
            String metricName = currentKey + OCCURRENCES;
            baseMetrics.put(metricName, new Metric(metricName, String.valueOf(BigInteger.ZERO),
                    logMetrics.getMetricPrefix() + METRIC_SEPARATOR + metricName));
        }
    }

    private void incrementWordCountIfSearchStringMatched(List<SearchPattern> searchPatterns,
                                                         String stringToCheck, LogMetrics logMetrics) {
        for (SearchPattern searchPattern : searchPatterns) {
            Matcher matcher = searchPattern.getPattern().matcher(stringToCheck);
            String logMetricPrefix = getSearchStringPrefix();
            String currentKey = logMetricPrefix + searchPattern.getDisplayName() + METRIC_SEPARATOR;

            if (!logMetrics.getMetricMap().containsKey(currentKey + OCCURRENCES)) {
                String metricName = currentKey + OCCURRENCES;
                logMetrics.add(metricName, new Metric(metricName, String.valueOf(BigInteger.ZERO),
                        logMetrics.getMetricPrefix() + METRIC_SEPARATOR + metricName));
            }

            while (matcher.find()) {
                BigInteger occurrences = new BigInteger(logMetrics.getMetricMap().get(currentKey + OCCURRENCES)
                        .getMetricValue());
                LOGGER.info("Match found for pattern: {} in log: {}. Incrementing occurrence count for metric: {}",
                        log.getDisplayName(), stringToCheck, currentKey);
                String metricName = currentKey + OCCURRENCES;
                logMetrics.add(metricName, new Metric(metricName, String.valueOf(occurrences.add(BigInteger.ONE)),
                        logMetrics.getMetricPrefix() + METRIC_SEPARATOR + metricName));

                if (searchPattern.getPrintMatchedString()) {
                    LOGGER.info("Adding actual matches to the queue for printing for log: {}", log.getDisplayName());
                    String replacedWord = applyReplacers(matcher.group().trim());
                    if (searchPattern.getCaseSensitive()) {
                        metricName = currentKey + MATCHES + replacedWord;
                    } else {
                        metricName = currentKey + MATCHES + WordUtils.capitalizeFully(replacedWord);
                    }
                    logMetrics.add(metricName, logMetrics.getMetricPrefix() + METRIC_SEPARATOR + metricName);
                }
            }
        }
    }

    private void updateCurrentFilePointer(String filePath, long lastReadPosition, long creationTimestamp) {
        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(filePath);
        filePointer.setFileCreationTime(creationTimestamp);
        filePointer.updateLastReadPosition(lastReadPosition);
        logMetrics.updateFilePointer(filePointer);
    }

    private String getSearchStringPrefix() {
        return String.format("%s%s%s", getLogNamePrefix(),
                SEARCH_STRING, METRIC_SEPARATOR);
    }

    private String applyReplacers(String name) {
        if (name == null || name.length() == 0 || replacers == null) {
            return name;
        }
        for (Map.Entry<Pattern, String> replacerEntry : replacers.entrySet()) {
            Pattern pattern = replacerEntry.getKey();
            Matcher matcher = pattern.matcher(name);
            name = matcher.replaceAll(replacerEntry.getValue());
        }
        return name;
    }

    private String getLogNamePrefix() {
        String displayName = StringUtils.isBlank(log.getDisplayName()) ?
                log.getLogName() : log.getDisplayName();
        return displayName + METRIC_SEPARATOR;
    }
}