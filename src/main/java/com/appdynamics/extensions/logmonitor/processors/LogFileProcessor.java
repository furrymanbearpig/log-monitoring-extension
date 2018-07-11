/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.processors;

import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.config.SearchPattern;
import com.appdynamics.extensions.logmonitor.metrics.LogMetrics;
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

import static com.appdynamics.extensions.logmonitor.util.Constants.FILESIZE_METRIC_NAME;
import static com.appdynamics.extensions.logmonitor.util.Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.logmonitor.util.Constants.SEARCH_STRING;
import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.closeRandomAccessFile;
import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.createPattern;
import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.getCurrentFileCreationTimeStamp;

/**
 * Created by aditya.jagtiani on 6/18/18.
 */

public class LogFileProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(LogFileProcessor.class);
    private OptimizedRandomAccessFile randomAccessFile;
    private Log log;
    private CountDownLatch latch;
    private LogMetrics logMetrics;
    private File currentFile;
    private List<SearchPattern> searchPatterns;

    public LogFileProcessor(OptimizedRandomAccessFile randomAccessFile, Log log, CountDownLatch latch, LogMetrics logMetrics, File currentFile) {
        this.randomAccessFile = randomAccessFile;
        this.log = log;
        this.latch = latch;
        this.logMetrics = logMetrics;
        this.currentFile = currentFile;
        this.searchPatterns = createPattern(this.log.getSearchStrings());
    }

    public void run() {
        try {
            processLogFile();
        }
        catch(Exception ex) {
            logger.error("Error encountered while processing log file : {}" ,log.getDisplayName());
        }
        finally{
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
        long curFileCreationTime = getCurrentFileCreationTimeStamp(currentFile);
        logMetrics.add(getLogNamePrefix() + FILESIZE_METRIC_NAME, BigInteger.valueOf(randomAccessFile.length()));
        updateCurrentFilePointer(currentFile.getPath(), currentFilePointer, curFileCreationTime);
        logger.info(String.format("Successfully processed log file [%s]",
                randomAccessFile));
    }

    private void incrementWordCountIfSearchStringMatched(List<SearchPattern> searchPatterns,
                                                         String stringToCheck, LogMetrics logMetrics) throws Exception {
        for (SearchPattern searchPattern : searchPatterns) {
            Matcher matcher = searchPattern.getPattern().matcher(stringToCheck);
            String logMetricPrefix = getSearchStringPrefix();
            String currentKey = logMetricPrefix + searchPattern.getDisplayName() + METRIC_PATH_SEPARATOR
                    + "Occurrences";
            if (!logMetrics.getMetrics().containsKey(currentKey)) {
                logMetrics.add(currentKey, BigInteger.ZERO);
            }
            while (matcher.find()) {
                BigInteger globalSeedCount = logMetrics.getMetrics().get(currentKey);
                logMetrics.add(currentKey, globalSeedCount.add(BigInteger.ONE));
                String word = matcher.group().trim();
                String replacedWord = applyReplacers(word);
                if (searchPattern.getPrintMatchedString()) {
                    if (searchPattern.getCaseSensitive()) {
                        logMetrics.add(logMetricPrefix + searchPattern.getDisplayName() + METRIC_PATH_SEPARATOR +
                                "Matches" + METRIC_PATH_SEPARATOR + replacedWord);
                    } else {
                        logMetrics.add(logMetricPrefix + searchPattern.getDisplayName() + METRIC_PATH_SEPARATOR +
                                "Matches" + METRIC_PATH_SEPARATOR + WordUtils.capitalizeFully(replacedWord));
                    }
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
                SEARCH_STRING, METRIC_PATH_SEPARATOR);
    }

    // TODO
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

        return displayName + METRIC_PATH_SEPARATOR;
    }
}
