/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.processors;

import com.appdynamics.extensions.MonitorExecutorService;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.config.SearchPattern;
import com.appdynamics.extensions.logmonitor.metrics.LogMetrics;
import com.appdynamics.extensions.logmonitor.util.LogMonitorUtil;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.bitbucket.kienerj.OptimizedRandomAccessFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.*;

/**
 * Created by aditya.jagtiani on 7/3/18.
 */
public class LogFileManager {
    private static final Logger logger = LoggerFactory.getLogger(LogFileManager.class);
    private Log log;
    private FilePointerProcessor filePointerProcessor;
    private MonitorContextConfiguration monitorContextConfiguration;
    private MonitorExecutorService executorService;

    public LogFileManager(FilePointerProcessor filePointerProcessor, Log log, MonitorContextConfiguration monitorContextConfiguration) {
        this.log = log;
        this.filePointerProcessor = filePointerProcessor;
        this.monitorContextConfiguration = monitorContextConfiguration;
        this.executorService = this.monitorContextConfiguration.getContext().getExecutorService();
    }

    public LogMetrics getLogMetrics() {
        logger.info("Starting the Log Monitoring Task for log : {}", log.getDisplayName());
        String dirPath = resolveDirPath(log.getLogDirectory());
        OptimizedRandomAccessFile randomAccessFile; File file = null;
        LogMetrics logMetrics = new LogMetrics();

        try {
            List<File> filesToBeProcessed; CountDownLatch latch;
            long currentFilePointerPosition = 0;
            file = getLogFile(dirPath);
            String dynamicLogPath = dirPath + log.getLogName();
            long curTimeStampFromFilePointer = getCurrentTimeStampFromFilePointer(dynamicLogPath, file.getPath());
// todo Work on metric char replacers
// todo utf 16
            Map<Pattern, String> replacers = getMetricCharacterReplacers();

            if (hasLogRolledOver(dynamicLogPath, file.getPath(), file.length())) { // logs rolled over
                filesToBeProcessed = getRequiredFilesFromDir(curTimeStampFromFilePointer, dirPath);
                latch = new CountDownLatch(filesToBeProcessed.size());
                for (File curFile : filesToBeProcessed) {
                    randomAccessFile = new OptimizedRandomAccessFile(curFile, "r");
                    currentFilePointerPosition = getCurrentFilePointerOffset(dynamicLogPath, file.getPath(), file.length(),
                            curTimeStampFromFilePointer, curFile);
                    randomAccessFile.seek(currentFilePointerPosition);
                    LogFileProcessor logFileProcessor = new LogFileProcessor(randomAccessFile, log, latch, logMetrics, curFile, rep);
                    executorService.execute("LogFileProcessor", logFileProcessor);
                }
            } else { // when the log has not rolled over
                randomAccessFile = new OptimizedRandomAccessFile(file, "r");
                randomAccessFile.seek(currentFilePointerPosition);
                latch = new CountDownLatch(1);
                LogFileProcessor logFileProcessor = new LogFileProcessor(randomAccessFile, log, latch, logMetrics, file);
                executorService.execute("LogFileProcessor", logFileProcessor);
            }

            latch.await();
            setNewFilePointer(dynamicLogPath, logMetrics.getFilePointers());

        } catch (Exception e) {
            logger.error("File I/O issue while processing : " + file.getAbsolutePath(), e);
        }
        return logMetrics;
    }


    private void setNewFilePointer(String dynamicLogPath, CopyOnWriteArrayList<FilePointer> filePointers) {

        FilePointer latestFilePointer = LogMonitorUtil.getLatestFilePointer(filePointers);
        filePointerProcessor.updateFilePointer(dynamicLogPath, latestFilePointer.getFilename(),
                latestFilePointer.getLastReadPosition(), latestFilePointer.getFileCreationTime());
    }

    private String resolveDirPath(String confDirPath) {
        String resolvedPath = resolvePath(confDirPath);

        if (!resolvedPath.endsWith(File.separator)) {
            resolvedPath = resolvedPath + File.separator;
        }
        return resolvedPath;
    }

    private long getCurrentTimeStampFromFilePointer(String dynamicLogPath, String actualLogPath) {
        FilePointer filePointer =
                filePointerProcessor.getFilePointer(dynamicLogPath, actualLogPath);
        return filePointer.getFileCreationTime();
    }

    private List<File> getRequiredFilesFromDir(long curTimeStampFromFilePtr, String path) throws IOException {
        List<File> filesToBeProcessed = Lists.newArrayList();
        File directory = new File(path);

        if (directory.isDirectory()) {
            FileFilter fileFilter = new WildcardFileFilter(log.getLogName());
            File[] files = directory.listFiles(fileFilter);

            if (files != null && files.length > 0) {
                for (File file : files) {
                    long curFileCreationTime = getCurrentFileCreationTimeStamp(file);
                    if (curFileCreationTime >= curTimeStampFromFilePtr) {
                        filesToBeProcessed.add(file);
                    }
                }
            }
        } else {
            throw new FileNotFoundException(
                    String.format("Directory [%s] not found. Ensure it is a directory.",
                            path));
        }
        return filesToBeProcessed;
    }

    private File getLogFile(String dirPath) throws Exception {
        File directory = new File(dirPath);
        File logFile;

        if (directory.isDirectory()) {
            FileFilter fileFilter = new WildcardFileFilter(log.getLogName());
            File[] files = directory.listFiles(fileFilter);

            if (files != null && files.length > 0) {
                logFile = getLatestFile(files);

                if (!logFile.canRead()) {
                    throw new IOException(
                            String.format("Unable to read file [%s]", logFile.getPath()));
                }

            } else {
                throw new FileNotFoundException(
                        String.format("Unable to find any file with name [%s] in [%s]",
                                log.getLogName(), dirPath));
            }

        } else {
            throw new FileNotFoundException(
                    String.format("Directory [%s] not found. Ensure it is a directory.",
                            dirPath));
        }

        return logFile;
    }

    private File getLatestFile(File[] files) {
        File latestFile = null;
        long lastModified = Long.MIN_VALUE;

        for (File file : files) {
            if (file.lastModified() > lastModified) {
                latestFile = file;
                lastModified = file.lastModified();
            }
        }
        return latestFile;
    }

    private boolean isLogRotated(long fileSize, long startPosition) {
        return fileSize < startPosition;
    }

    private boolean isFilenameChanged(String oldFilename, String newFilename) {
        return !oldFilename.equals(newFilename);
    }

    private boolean hasLogRolledOver(String dynamicLogPath, String actualLogPath, long fileSize) {
        FilePointer filePointer =
                filePointerProcessor.getFilePointer(dynamicLogPath, actualLogPath);
        long currentPosition = filePointer.getLastReadPosition().get();
        if (isFilenameChanged(filePointer.getFilename(), actualLogPath) ||
                isLogRotated(fileSize, currentPosition)) {
            logger.debug("Filename has either changed or rotated, resetting position to 0");
            return true;
        }
        return false;
    }

    private long getCurrentFilePointerOffset(String dynamicLogPath, String actualLogPath, long fileSize,
                                             long curTimeStampFromFilePointer, File curFile) throws Exception {
        FilePointer filePointer =
                filePointerProcessor.getFilePointer(dynamicLogPath, actualLogPath);

        long currentPosition = filePointer.getLastReadPosition().get();
        if (getCurrentFileCreationTimeStamp(curFile) == curTimeStampFromFilePointer) {
            // found the oldest file, start from CFP
            return currentPosition;
        } else {
            // start from 0
            return 0;
        }
    }

    private Map<Pattern, String> getMetricCharacterReplacers() {
        List<Map<String, String>> metricCharReplacersFromCfg = (List) monitorContextConfiguration.getConfigYml().get("metricCharacterReplacers");
        return initializeMetricCharacterReplacers(metricCharReplacersFromCfg);
    }
}
