/*
 *  Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor;

import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.processors.FilePointer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.bitbucket.kienerj.OptimizedRandomAccessFile;

import java.io.File;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.appdynamics.extensions.logmonitor.Constants.*;
import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.getCurrentFileCreationTimeStamp;

/**
 * Created by aditya.jagtiani on 8/21/17.
 */
public class ThreadedFileProcessor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ThreadedFileProcessor.class);
    private CountDownLatch countDownLatch;
    private Log log;
    private OptimizedRandomAccessFile randomAccessFile;
    private LogMetrics logMetrics;
    private Map<Pattern, String> replacers;
    private long curFilePointer;
    private File currentFile;
    private List<SearchPattern> searchPatterns;

    ThreadedFileProcessor(OptimizedRandomAccessFile randomAccessFile, Log log, CountDownLatch countDownLatch,
                          LogMetrics logMetrics, Map<Pattern, String> replacers, File currentFile,
                          List<SearchPattern> searchPatterns) {
        this.randomAccessFile = randomAccessFile;
        this.log = log;
        this.countDownLatch = countDownLatch;
        this.logMetrics = logMetrics;
        this.replacers = replacers;
        this.currentFile = currentFile;
        this.searchPatterns = searchPatterns;
    }

    public void run() {
        if (LOGGER.isDebugEnabled()) {
            for (SearchPattern searchPattern : searchPatterns) {
                LOGGER.debug(String.format("Searching for [%s]", searchPattern.getPattern().pattern()));
            }
        }
        try {
            processCurrentFile(searchPatterns);
        } catch (Exception e) {
            LOGGER.debug("An error has occurred in the Log Monitoring Task : ", e);
        }
    }

    private void processCurrentFile(List<SearchPattern> searchPatterns) throws Exception {
        String currentLine;
        curFilePointer = randomAccessFile.getFilePointer();
        while ((currentLine = randomAccessFile.readLine()) != null) {
            incrementWordCountIfSearchStringMatched(searchPatterns, currentLine, logMetrics);
            curFilePointer = randomAccessFile.getFilePointer();
        }
        long curFileCreationTime = getCurrentFileCreationTimeStamp(currentFile);
        logMetrics.add(getLogNamePrefix() + FILESIZE_METRIC_NAME, BigInteger.valueOf(randomAccessFile.length()));
        updateCurrentFilePointer(currentFile.getPath(), curFilePointer, curFileCreationTime);
        LOGGER.info(String.format("Successfully processed log file [%s]",
                randomAccessFile));
        countDownLatch.countDown();
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
