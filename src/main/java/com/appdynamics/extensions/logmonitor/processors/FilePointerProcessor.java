/*
 *  Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.processors;

import static com.appdynamics.extensions.logmonitor.util.Constants.FILEPOINTER_FILENAME;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.appdynamics.extensions.logmonitor.LogMonitor;

/**
 * @author Florencio Sarmiento
 *
 */
public class FilePointerProcessor {

    static final Logger LOGGER = Logger.getLogger(FilePointerProcessor.class);
    private ConcurrentHashMap<String, FilePointer> filePointers = new ConcurrentHashMap<String, FilePointer>();
    private ObjectMapper mapper = new ObjectMapper();

    public FilePointerProcessor() {
        initializeFilePointers();
    }

    void updateFilePointer(String dynamicLogPath,
                                  String actualLogPath, AtomicLong lastReadPosition, long creationTimestamp) {
        FilePointer filePointer = getFilePointer(dynamicLogPath, actualLogPath);
        filePointer.setFilename(actualLogPath);
        filePointer.setLastReadPosition(lastReadPosition);
        filePointer.setFileCreationTime(creationTimestamp);
    }

    FilePointer getFilePointer(String dynamicLogPath, String actualLogPath) {
        if (filePointers.containsKey(dynamicLogPath)) {
            return filePointers.get(dynamicLogPath);
        }

        FilePointer newFilePointer = new FilePointer();
        newFilePointer.setFilename(actualLogPath);

        FilePointer previousFilePointer = filePointers.putIfAbsent(dynamicLogPath, newFilePointer);
        return previousFilePointer != null ? previousFilePointer : newFilePointer;
    }

    public void updateFilePointerFile() {
        File file = new File(getFilePointerPath());

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, filePointers);

        } catch (Exception ex) {
            LOGGER.error(String.format(
                    "Unfortunately an error occurred while saving filepointers to %s",
                    file.getPath()), ex);
        }
    }

    private void initializeFilePointers() {
        LOGGER.info("Initialising filepointers...");
        File file = new File(getFilePointerPath());
        if (!file.exists()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to find: " + file.getPath());
            }
        } else {
            try {
                filePointers = mapper.readValue(file,
                        new TypeReference<ConcurrentHashMap<String, FilePointer>>() {
                        });
            } catch (Exception ex) {
                LOGGER.error(String.format(
                        "Unfortunately an error occurred while reading filepointer %s",
                        file.getPath()), ex);
            }
        }
        LOGGER.info("Filepointers initialized with: " + filePointers);
    }

    private String getFilePointerPath() {
        String path = null;
        try {
            URL classUrl = LogMonitor.class.getResource(
                    LogMonitor.class.getSimpleName() + ".class");
            String jarPath = classUrl.toURI().toString();

            // workaround for jar file
            jarPath = jarPath.replace("jar:", "").replace("file:", "");
            if (jarPath.contains("!")) {
                jarPath = jarPath.substring(0, jarPath.indexOf("!"));
            }
            File file = new File(jarPath);
            String jarDir = file.getParentFile().toURI().getPath();

            if (jarDir.endsWith(File.separator)) {
                path = jarDir + FILEPOINTER_FILENAME;

            } else {
                path = String.format("%s%s%s", jarDir ,
                        File.separator, FILEPOINTER_FILENAME);
            }
        } catch (Exception ex) {
            LOGGER.warn("Unable to resolve installation dir, finding an alternative.");
        }

        if (StringUtils.isBlank(path)) {
            path = String.format("%s%s%s", new File(".").getAbsolutePath(),
                    File.separator, FILEPOINTER_FILENAME);
        }
        try {
            path = URLDecoder.decode(path, "UTF-8");

        } catch (UnsupportedEncodingException e) {
            LOGGER.warn(String.format("Unable to decode file path [%s] using UTF-8", path));
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Filepointer path: " + path);
        }
        return path;
    }
}