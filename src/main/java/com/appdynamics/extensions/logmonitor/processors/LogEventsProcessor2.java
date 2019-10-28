package com.appdynamics.extensions.logmonitor.processors;

import com.appdynamics.extensions.eventsservice.EventsServiceDataManager;
import com.appdynamics.extensions.logmonitor.LogEvent;
import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.config.SearchPattern;
import com.sun.xml.internal.ws.developer.SerializationFeature;
import org.apache.commons.io.FileUtils;
import org.bitbucket.kienerj.OptimizedRandomAccessFile;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.appdynamics.extensions.logmonitor.util.Constants.SCHEMA_NAME;

public class LogEventsProcessor2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogEventsProcessor2.class);
    private EventsServiceDataManager eventsServiceDataManager;
    private int offset;
    private List<LogEvent> eventsToBePublished;
    private Log log;

    public LogEventsProcessor2(EventsServiceDataManager eventsServiceDataManager, int offset, Log log) {
        this.eventsServiceDataManager = eventsServiceDataManager;
        this.offset = offset;
        this.eventsToBePublished = new CopyOnWriteArrayList<LogEvent>();
        this.log = log;
    }

    public List<LogEvent> processLogEvents(SearchPattern searchPattern, OptimizedRandomAccessFile currentFile, String currentMatch) {
        try {
            createLogSchema();
            eventsToBePublished.add(createLogEvent(searchPattern, currentFile, currentMatch, offset));
        } catch (Exception ex) {
            LOGGER.error("The events service data manager failed to initialize. Check your config.yml and retry.");
        }
        return eventsToBePublished;
    }

    private void createLogSchema() throws Exception {
        if (com.appdynamics.extensions.util.StringUtils.hasText(eventsServiceDataManager.retrieveSchema(SCHEMA_NAME))) {
            LOGGER.info("Schema: {} already exists", SCHEMA_NAME);
        } else {
            eventsServiceDataManager.createSchema(SCHEMA_NAME, FileUtils.readFileToString(new File("src/main/" +
                    "resources/eventsService/logSchema.json")));
        }
    }

    private LogEvent createLogEvent(SearchPattern searchPattern, OptimizedRandomAccessFile randomAccessFile,
                                    String currentMatch, int offset) {
        try {
            LogEvent logEvent = new LogEvent();
            logEvent.setLogDisplayName(log.getDisplayName());
            logEvent.setSearchPattern(searchPattern.getDisplayName());
            if (offset > 0) {
                OptimizedRandomAccessFile randomAccessFileCopy = randomAccessFile;
                for (int i = 0; i < offset; i++) {
                    currentMatch += randomAccessFileCopy.readLine();
                }
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

    public void publishEvents(List<LogEvent> eventsToBePublished) {
        List<String> events = prepareEventsForPublishing(eventsToBePublished);
        eventsServiceDataManager.publishEvents(SCHEMA_NAME, events);
    }

    private CopyOnWriteArrayList<String> prepareEventsForPublishing(List<LogEvent> eventsToBePublished) {
        CopyOnWriteArrayList<String> events = new CopyOnWriteArrayList<String>();
        ObjectMapper mapper = new ObjectMapper();
        for (LogEvent logEvent : eventsToBePublished) {
            try {
                events.add(mapper.writeValueAsString(logEvent));
            } catch (Exception ex) {
                LOGGER.error("Error encountered while publishing LogEvent {} for log {}", logEvent, log.getDisplayName(), ex);
            }
        }
        return events;
    }
}
