package com.appdynamics.extensions.logmonitor;

import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.processors.FilePointer;
import com.appdynamics.extensions.logmonitor.processors.FilePointerProcessor;
import com.google.common.collect.Lists;
import org.bitbucket.kienerj.OptimizedRandomAccessFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.closeRandomAccessFile;
import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.createPattern;
import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.resolvePath;

/**
 * Created by aditya.jagtiani on 9/12/17.
 */
public class NewLogMonitorTask implements Callable<LogMetrics> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewLogMonitorTask.class);
    private FilePointerProcessor filePointerProcessor;
    private Log log;
    private Map<Pattern, String> replacers;
    private ExecutorService executorService;

    public NewLogMonitorTask(FilePointerProcessor filePointerProcessor, Log log, Map<Pattern, String> replacers, ExecutorService executorService) {
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
            File file = new File(dirPath);
            randomAccessFile = new OptimizedRandomAccessFile(file, "r");
            List<File> filesToBeProcessed;
            long curFileCreationTime = getCurrentFileCreationTimeStamp(file);
            long curTimeStampFromFilePointer = getCurrentTimeStampFromFilePointer(dirPath + log.getLogName(), file.getPath());
            curFilePointer = getCurrentFilePointer(dirPath + log.getLogName(), file.getPath(), randomAccessFile.length());
            if (curFileCreationTime > curTimeStampFromFilePointer) { // there is a rollover
                filesToBeProcessed = getRequiredFilesFromDir(curTimeStampFromFilePointer, dirPath);
                CountDownLatch latch = new CountDownLatch(filesToBeProcessed.size());
                for (File curFile : filesToBeProcessed) {
                    if (getCurrentFileCreationTimeStamp(curFile) == curTimeStampFromFilePointer) {// found the oldest file, start from CFP
                        randomAccessFile.seek(curFilePointer);
                    } else {
                        // start from 0
                        randomAccessFile.seek(0);
                    }
                    executorService.execute(new ThreadedFileProcessor(randomAccessFile, log, latch, logMetrics, replacers));
                }
                latch.await();
            }
        } finally {
            closeRandomAccessFile(randomAccessFile);
        }
        return logMetrics;
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

    private long getCurrentFileCreationTimeStamp(File file) throws IOException {
        Path p = Paths.get(file.getAbsolutePath());
        BasicFileAttributes view
                = Files.getFileAttributeView(p, BasicFileAttributeView.class)
                .readAttributes();
        return view.creationTime().toMillis();
    }

    private List<File> getRequiredFilesFromDir(long curTimeStampFromFilePtr, String path) throws IOException {
        List<File> filesToBeProcessed = Lists.newArrayList();
        File[] files = new File(path).listFiles();
        for (File file : files) {
            if (getCurrentFileCreationTimeStamp(file) >= curTimeStampFromFilePtr) {
                filesToBeProcessed.add(file);
            }
        }
        return filesToBeProcessed;
    }

    private long getCurrentFilePointer(String dynamicLogPath,
                                       String actualLogPath, long fileSize) {

        FilePointer filePointer =
                filePointerProcessor.getFilePointer(dynamicLogPath, actualLogPath);

        long currentPosition = filePointer.getLastReadPosition().get();

/*        if (isFilenameChanged(filePointer.getFilename(), actualLogPath) ||
                isLogRotated(fileSize, currentPosition)) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Filename has either changed or rotated, resetting position to 0");
            }

            currentPosition = 0;
        }*/

        return currentPosition;
    }

/*
    private boolean isLogRotated(long fileSize, long startPosition) {
        return fileSize < startPosition;
    }

    private boolean isFilenameChanged(String oldFilename, String newFilename) {
        return !oldFilename.equals(newFilename);
    }
*/

}
