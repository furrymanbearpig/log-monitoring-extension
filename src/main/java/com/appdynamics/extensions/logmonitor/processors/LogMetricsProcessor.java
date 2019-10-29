/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.processors;

import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.eventsservice.EventsServiceDataManager;
import com.appdynamics.extensions.logmonitor.LogEvent;
import com.appdynamics.extensions.logmonitor.config.FilePointer;
import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.config.SearchPattern;
import com.appdynamics.extensions.logmonitor.metrics.LogMetrics;
import com.appdynamics.extensions.metrics.Metric;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bitbucket.kienerj.OptimizedRandomAccessFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.appdynamics.extensions.logmonitor.LogMonitor.metrics;
import static com.appdynamics.extensions.logmonitor.util.Constants.*;
import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.*;

/**
 * @author Aditya Jagtiani
 */

public class LogMetricsProcessor implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogMetricsProcessor.class);
    private OptimizedRandomAccessFile randomAccessFile;
    private Log log;
    private CountDownLatch latch;
    private File currentFile;
    private List<SearchPattern> searchPatterns;
    private Map<Pattern, String> replacers;
    private LogMetrics logMetrics;
    private MonitorContextConfiguration monitorContextConfiguration;
    private boolean isEventsServiceEnabled;
    private EventsServiceDataManager eventsServiceDataManager;
    private LogEventsProcessor2 logEventsProcessor;
    private List<LogEvent> eventsToBePublished;

    LogMetricsProcessor(OptimizedRandomAccessFile randomAccessFile, Log log, CountDownLatch latch, LogMetrics logMetrics,
                        File currentFile, Map<Pattern, String> replacers, MonitorContextConfiguration monitorContextConfiguration) {
        this.randomAccessFile = randomAccessFile;
        this.log = log;
        this.latch = latch;
        this.logMetrics = logMetrics;
        this.currentFile = currentFile;
        this.replacers = replacers;
        this.searchPatterns = createPattern(this.log.getSearchStrings());
        this.monitorContextConfiguration = monitorContextConfiguration;
        isEventsServiceEnabled = (Boolean) this.monitorContextConfiguration.getConfigYml().get("sendDataToEventsService");

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
        setBaseOccurrenceCountForConfiguredPatterns();
        eventsServiceDataManager = evaluateEventsServiceConfig();
        if(eventsServiceDataManager != null) {
            int offset = (Integer) this.monitorContextConfiguration.getConfigYml().get("logMatchOffset");
            logEventsProcessor = new LogEventsProcessor2(eventsServiceDataManager, offset, log);
            eventsToBePublished = new CopyOnWriteArrayList<LogEvent>();
        }
        while ((currentLine = randomAccessFile.readLine()) != null) {
            incrementWordCountIfSearchStringMatched(searchPatterns, currentLine);
            currentFilePointer = randomAccessFile.getFilePointer();
        }
        long currentFileCreationTime = getCurrentFileCreationTimeStamp(currentFile);
        String metricName = getLogNamePrefix() + FILESIZE_METRIC_NAME;
        logMetrics.add(metricName, new Metric(metricName,
                String.valueOf(randomAccessFile.length()), logMetrics.getMetricPrefix() + METRIC_SEPARATOR
                + metricName));
        updateCurrentFilePointer(currentFile.getPath(), currentFilePointer, currentFileCreationTime);
        LOGGER.info(String.format("Successfully processed log file [%s]",
                randomAccessFile));
    }

    private void setBaseOccurrenceCountForConfiguredPatterns() {
        for (SearchPattern searchPattern : searchPatterns) {
            String currentKey = getSearchStringPrefix() + searchPattern.getDisplayName() + METRIC_SEPARATOR;
            if (!metrics.containsKey(currentKey + OCCURRENCES)) {
                String metricName = currentKey + OCCURRENCES;
                logMetrics.add(metricName, new Metric(metricName, String.valueOf(BigInteger.ZERO),
                        logMetrics.getMetricPrefix() + METRIC_SEPARATOR + metricName));
            }
        }
    }

    private void incrementWordCountIfSearchStringMatched(List<SearchPattern> searchPatterns, String stringToCheck) {
        for (SearchPattern searchPattern : searchPatterns) {
            Matcher matcher = searchPattern.getPattern().matcher(stringToCheck);
            String currentKey = getSearchStringPrefix() + searchPattern.getDisplayName() + METRIC_SEPARATOR;

            while (matcher.find()) {
                BigInteger occurrences = new BigInteger(metrics.get(currentKey + OCCURRENCES)
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

                //TODO move events service code here.
                if (logEventsProcessor != null) {
                    eventsToBePublished.addAll(logEventsProcessor.processLogEvents(searchPattern, randomAccessFile, stringToCheck));
                    logEventsProcessor.publishEvents(eventsToBePublished);
                } else {
                    LOGGER.info("This data does not have to be sent to the events service, skipping.");
                }
            }
        }
    }

    private EventsServiceDataManager evaluateEventsServiceConfig() {
        if(isEventsServiceEnabled) {
            return monitorContextConfiguration.getContext().getEventsServiceDataManager();
        }
        return null;
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