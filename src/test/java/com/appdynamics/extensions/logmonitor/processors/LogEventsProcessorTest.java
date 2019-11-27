package com.appdynamics.extensions.logmonitor.processors;

import com.appdynamics.extensions.eventsservice.EventsServiceDataManager;
import com.appdynamics.extensions.logmonitor.LogEvent;
import com.appdynamics.extensions.logmonitor.config.FilePointer;
import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.config.SearchPattern;
import org.bitbucket.kienerj.OptimizedRandomAccessFile;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.regex.Pattern;

import static com.appdynamics.extensions.logmonitor.util.Constants.SCHEMA_NAME;

public class LogEventsProcessorTest {


    @Test
    public void testLogEventGeneratorWithOffset() throws Exception {
        EventsServiceDataManager eventsServiceDataManager = Mockito.mock(EventsServiceDataManager.class);
        Mockito.when(eventsServiceDataManager.retrieveSchema(SCHEMA_NAME)).thenReturn("Hello world");
        Log log = new Log();
        log.setDisplayName("TestLog");
        log.setLogDirectory("src/test/resources/");
        log.setLogName("test-log-4-events-service.log");

        SearchPattern searchPattern = new SearchPattern("Test Patterns", Pattern.compile("1"), false, false );

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + log.getLogName());

        OptimizedRandomAccessFile randomAccessFile = new OptimizedRandomAccessFile(new File("src/test/resources/test-log-4-events-service.log"), "r");

        int offset = 5;

        LogEventsProcessor classUnderTest = new LogEventsProcessor(eventsServiceDataManager, offset, log);
        LogEvent logEvent = classUnderTest.processLogEvent(searchPattern, randomAccessFile, "");

        Assert.assertEquals("1\n1\n2\n3\n4\n", logEvent.getLogMatch());
    }
}
