package com.appdynamics.extensions.logmonitor.processors;

import com.appdynamics.extensions.eventsservice.EventsServiceDataManager;
import com.appdynamics.extensions.logmonitor.LogEvent;
import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.config.SearchPattern;
import com.appdynamics.extensions.util.StringUtils;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.bitbucket.kienerj.OptimizedRandomAccessFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;

import static com.appdynamics.extensions.logmonitor.util.Constants.SCHEMA_NAME;
import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.closeRandomAccessFile;
import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.createPattern;

public class LogEventsProcessor implements Runnable {
    private OptimizedRandomAccessFile randomAccessFile;
    private static final Logger LOGGER = LoggerFactory.getLogger(LogEventsProcessor.class);
    private Log log;
    private CountDownLatch latch;
    private File currentFile;
    private List<SearchPattern> searchPatterns;
    private int offset;
    private EventsServiceDataManager eventsServiceDataManager;
    private List<LogEvent> eventsToBePublished;

    LogEventsProcessor(EventsServiceDataManager eventsServiceDataManager, OptimizedRandomAccessFile randomAccessFile,
                              Log log, CountDownLatch latch, File currentFile, int offset) {
        this.randomAccessFile = randomAccessFile;
        this.log = log;
        this.latch = latch;
        this.currentFile = currentFile;
        this.searchPatterns = createPattern(this.log.getSearchStrings());
        this.offset = offset;
        this.eventsServiceDataManager = eventsServiceDataManager;
        this.eventsToBePublished = Lists.newCopyOnWriteArrayList();
    }

    public void run() {
        try {
            createLogSchema();
            processLogFile();
        } catch (Exception ex) {
            LOGGER.error("Error encountered while processing events for log file : {}", log.getDisplayName(), ex);
        } finally {
            closeRandomAccessFile(randomAccessFile);
            latch.countDown();
        }
    }

    private void createLogSchema() throws Exception {
        if(StringUtils.hasText(eventsServiceDataManager.retrieveSchema(SCHEMA_NAME))) {
            LOGGER.info("Schema: {} already exists", SCHEMA_NAME);
        }
        else {
            eventsServiceDataManager.createSchema(SCHEMA_NAME, FileUtils.readFileToString(new File("src/main/" +
                    "resources/eventsService/logSchema.json")));
        }
    }

    private void processLogFile() throws Exception {
        long currentFilePointer = randomAccessFile.getFilePointer();
        String currentLine;
        while((currentLine = randomAccessFile.readLine()) != null) {
            for(SearchPattern searchPattern : searchPatterns) {
                Matcher matcher = searchPattern.getPattern().matcher(currentLine);
                while(matcher.find()) {
                    eventsToBePublished.add(generateEvent(searchPattern, randomAccessFile, currentLine));
                    currentFilePointer = randomAccessFile.getFilePointer();
                }

            }
        }
    }

    private LogEvent generateEvent(SearchPattern searchPattern, OptimizedRandomAccessFile randomAccessFile,
                                   String currentLine) throws Exception {
        LogEvent logEvent = new LogEvent();
        logEvent.setLogDisplayName(log.getDisplayName());
        String currentMatch = currentLine;
        logEvent.setSearchPattern(searchPattern.getDisplayName());
        if(offset > 0) {
            OptimizedRandomAccessFile randomAccessFileCopy = randomAccessFile;
            for(int i = 0; i < offset; i++) {
                currentMatch += randomAccessFileCopy.readLine();
            }
        }
        logEvent.setLogMatch(currentMatch);
        logEvent.setSearchPattern(searchPattern.getPattern().pattern());
        return logEvent;
    }
}
