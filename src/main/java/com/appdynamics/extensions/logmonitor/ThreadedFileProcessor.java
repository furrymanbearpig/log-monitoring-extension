package com.appdynamics.extensions.logmonitor;

import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.util.LogMonitorUtil;
import com.google.common.collect.Maps;
import jdk.nashorn.internal.codegen.CompilerConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.bitbucket.kienerj.OptimizedRandomAccessFile;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.appdynamics.extensions.logmonitor.Constants.FILESIZE_METRIC_NAME;
import static com.appdynamics.extensions.logmonitor.Constants.METRIC_PATH_SEPARATOR;
import static com.appdynamics.extensions.logmonitor.Constants.SEARCH_STRING;
import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.createPattern;

/**
 * Created by aditya.jagtiani on 8/21/17.
 */
public class ThreadedFileProcessor implements Runnable {
    private static final Logger LOGGER =
            Logger.getLogger(ThreadedFileProcessor.class);
    private CountDownLatch countDownLatch;
    private Log log;
    private OptimizedRandomAccessFile randomAccessFile;
    private LogMetrics logMetrics;
    private Map<Pattern, String> replacers;
    private long curFilePointer;

    public ThreadedFileProcessor(OptimizedRandomAccessFile randomAccessFile, Log log, CountDownLatch countDownLatch, LogMetrics logMetrics, Map<Pattern, String> replacers) {
        this.randomAccessFile = randomAccessFile;
        this.log = log;
        this.countDownLatch = countDownLatch;
        this.logMetrics = logMetrics;
        this.replacers = replacers;
    }

    public void run() {
        List<SearchPattern> searchPatterns = createPattern(log.getSearchStrings());
        if (LOGGER.isDebugEnabled()) {
            for (SearchPattern searchPattern : searchPatterns) {
                LOGGER.debug(String.format("Searching for [%s]", searchPattern.getPattern().pattern()));
            }
        }
        try {
            processCurrentFile(searchPatterns);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processCurrentFile(List<SearchPattern> searchPatterns) throws IOException {
        String currentLine;
        while ((currentLine = randomAccessFile.readLine()) != null) {
            incrementWordCountIfSearchStringMatched(searchPatterns, currentLine, logMetrics);
            curFilePointer = randomAccessFile.getFilePointer();
        }
        logMetrics.add(getLogNamePrefix() + FILESIZE_METRIC_NAME, BigInteger.valueOf(randomAccessFile.length()));
        LOGGER.info(String.format("Successfully processed log file [%s]",
                randomAccessFile));
        countDownLatch.countDown();
    }


    private void incrementWordCountIfSearchStringMatched(List<SearchPattern> searchPatterns,
                                                         String stringToCheck, LogMetrics logMetrics) {

        for (SearchPattern searchPattern : searchPatterns) {
            Boolean isPresent = false;
            Matcher matcher = searchPattern.getPattern().matcher(stringToCheck);
            String logMetricPrefix = getSearchStringPrefix();

            while (matcher.find()) {
                isPresent = true;
                String word = matcher.group().trim();

                String replacedWord = applyReplacers(word);

                if (searchPattern.getCaseSensitive()) {
                    logMetrics.add(logMetricPrefix + searchPattern.getDisplayName() + METRIC_PATH_SEPARATOR + replacedWord);

                } else {
                    logMetrics.add(logMetricPrefix + searchPattern.getDisplayName() + METRIC_PATH_SEPARATOR + WordUtils.capitalizeFully(replacedWord));
                }
            }
            if(!isPresent) {
                String metricPrefix = getSearchStringPrefix() + searchPattern.getDisplayName() + METRIC_PATH_SEPARATOR;
                List<String> metricNames = LogMonitorUtil.getNamesFromSearchStrings(log.getSearchStrings());
                String patternName = searchPattern.getCaseSensitive() ? applyReplacers(searchPattern.getPattern().pattern().trim())
                        : WordUtils.capitalizeFully(applyReplacers(searchPattern.getPattern().pattern().trim()));
                for(String metricName : metricNames) {
                    if(StringUtils.containsIgnoreCase(patternName, metricName) && !logMetrics.getMetrics().containsKey(metricPrefix + metricName)) {
                        logMetrics.add(metricPrefix + metricName, BigInteger.ZERO);
                    }
                }
            }
        }
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
