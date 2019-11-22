package com.appdynamics.extensions.logmonitor.processors;

import com.appdynamics.extensions.eventsservice.EventsServiceDataManager;
import com.appdynamics.extensions.logmonitor.LogEvent;
import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.config.SearchPattern;
import org.apache.commons.io.FileUtils;
import org.bitbucket.kienerj.OptimizedRandomAccessFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.appdynamics.extensions.logmonitor.util.Constants.SCHEMA_NAME;

public class LogEventsProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogEventsProcessor.class);
    private EventsServiceDataManager eventsServiceDataManager;
    private int offset;
    private Log log;

    public LogEventsProcessor(EventsServiceDataManager eventsServiceDataManager, int offset, Log log) {
        this.eventsServiceDataManager = eventsServiceDataManager;
        this.offset = offset;
        this.log = log;
        createLogSchema();
    }

    public LogEvent processLogEvent(SearchPattern searchPattern, OptimizedRandomAccessFile currentFile, String currentMatch) {
        try {
            return createLogEvent(searchPattern, currentFile, currentMatch, offset);
        } catch (Exception ex) {
            LOGGER.error("The events service data manager failed to initialize. Check your config.yml and retry.");
        }
        return null;
    }

    private void createLogSchema() {
        try {
            if (com.appdynamics.extensions.util.StringUtils.hasText(eventsServiceDataManager.retrieveSchema(SCHEMA_NAME))) {
                LOGGER.info("Schema: {} already exists", SCHEMA_NAME);
            } else {
                eventsServiceDataManager.createSchema(SCHEMA_NAME, FileUtils.readFileToString(new File("src/main/" +
                        "resources/eventsService/logSchema.json")));
            }
        }
        catch (Exception ex) {
            LOGGER.error("Error encountered while creating schema for log {}", log.getDisplayName(), ex);
        }
    }

    private LogEvent createLogEvent(SearchPattern searchPattern, OptimizedRandomAccessFile randomAccessFile,
                                    String currentMatch, int offset) {
        try {
            LogEvent logEvent = new LogEvent();
            logEvent.setLogDisplayName(log.getDisplayName());
            logEvent.setSearchPattern(searchPattern.getDisplayName());
            if (offset > 0) {
                StringBuilder sb = new StringBuilder(currentMatch);
                long originalFilePointerPosition = randomAccessFile.getFilePointer();
                for (int i = 0; i < offset; i++) {
                    sb.append(randomAccessFile.readLine()).append('\n');
                }
                currentMatch = sb.toString();
                randomAccessFile.seek(originalFilePointerPosition);
            }
            logEvent.setLogMatch(currentMatch);
            logEvent.setSearchPattern(searchPattern.getPattern().pattern());
            return logEvent;
        } catch (Exception ex) {
            LOGGER.error("Error encountered while generating event for log {} and search pattern {}",
                    log.getDisplayName(), searchPattern.getPattern().pattern(), ex);
        }
        return null;
    }
}
