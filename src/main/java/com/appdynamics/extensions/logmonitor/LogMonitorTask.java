/*
 *  Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor;

import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.exceptions.FileException;
import com.appdynamics.extensions.logmonitor.processors.FilePointer;
import com.appdynamics.extensions.logmonitor.processors.FilePointerProcessor;
import com.appdynamics.extensions.logmonitor.util.LogMonitorUtil;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.*;

/**
 * Created by aditya.jagtiani on 9/12/17.
 */
public class LogMonitorTask implements Callable<LogMetrics> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogMonitorTask.class);
    private FilePointerProcessor filePointerProcessor;
    private Log log;
    private Map<Pattern, String> replacers;
    private ExecutorService executorService;
    private boolean hasLogRolledOver = false;


    LogMonitorTask(FilePointerProcessor filePointerProcessor, Log log, Map<Pattern, String> replacers,
                   ExecutorService executorService) {
        this.filePointerProcessor = filePointerProcessor;
        this.log = log;
        this.replacers = replacers;
        this.executorService = executorService;
    }

    public LogMetrics call() throws Exception {
        String dirPath = resolveDirPath(log.getLogDirectory());
        LOGGER.info("Log monitor task started...");
        LogMetrics logMetrics = new LogMetrics();
        OptimizedRandomAccessFile randomAccessFile = null;
        long curFilePointer;

        try {
            File file = getLogFile(dirPath);
            randomAccessFile = new OptimizedRandomAccessFile(file, "r");
            List<File> filesToBeProcessed;
            CountDownLatch latch;
            String dynamicLogPath = dirPath + log.getLogName();
            curFilePointer = getCurrentFilePointerOffset(dynamicLogPath, file.getPath(), file.length());
            long curTimeStampFromFilePointer = getCurrentTimeStampFromFilePointer(dynamicLogPath, file.getPath());
            List<SearchPattern> searchPatterns = createPattern(log.getSearchStrings());
            if (hasLogRolledOver) {
                filesToBeProcessed = getRequiredFilesFromDir(curTimeStampFromFilePointer, dirPath);
                latch = new CountDownLatch(filesToBeProcessed.size());
                for (File curFile : filesToBeProcessed) {
                    randomAccessFile = new OptimizedRandomAccessFile(curFile, "r");
                    if (getCurrentFileCreationTimeStamp(curFile) == curTimeStampFromFilePointer) {
                        // found the oldest file, start from CFP
                        randomAccessFile.seek(curFilePointer);
                    } else {
                        // start from 0
                        randomAccessFile.seek(0);
                    }
                    executorService.execute(new ThreadedFileProcessor(randomAccessFile, log, latch, logMetrics,
                            replacers, curFile, searchPatterns));
                }
            } else { // when the log has not rolled over
                randomAccessFile.seek(curFilePointer);
                latch = new CountDownLatch(1);
                executorService.execute(new ThreadedFileProcessor(randomAccessFile, log, latch, logMetrics,
                        replacers, file, searchPatterns)
                );
            }
            latch.await();
            setNewFilePointer(dynamicLogPath, logMetrics.getFilePointers());
        } finally {
            closeRandomAccessFile(randomAccessFile);
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

    private long getCurrentFilePointerOffset(String dynamicLogPath,
                                             String actualLogPath, long fileSize) {

        FilePointer filePointer =
                filePointerProcessor.getFilePointer(dynamicLogPath, actualLogPath);

        long currentPosition = filePointer.getLastReadPosition().get();

        if (isFilenameChanged(filePointer.getFilename(), actualLogPath) ||
                isLogRotated(fileSize, currentPosition)) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Filename has either changed or rotated, resetting position to 0");
            }
            hasLogRolledOver = true;
        }
        return currentPosition;
    }

    private File getLogFile(String dirPath) throws FileNotFoundException {
        File directory = new File(dirPath);
        File logFile;

        if (directory.isDirectory()) {
            FileFilter fileFilter = new WildcardFileFilter(log.getLogName());
            File[] files = directory.listFiles(fileFilter);

            if (files != null && files.length > 0) {
                logFile = getLatestFile(files);

                if (!logFile.canRead()) {
                    throw new FileException(
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
}
