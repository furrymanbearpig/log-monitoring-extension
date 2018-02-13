
/*
 *  Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.config.SearchString;
import com.appdynamics.extensions.logmonitor.processors.FilePointer;
import com.appdynamics.extensions.logmonitor.processors.FilePointerProcessor;
import com.appdynamics.extensions.logmonitor.util.LogMonitorUtil;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

@RunWith(MockitoJUnitRunner.class)
public class LogMonitorTaskTest {

    private LogMonitorTask classUnderTest;

    @Mock
    private FilePointerProcessor mockFilePointerProcessor;

    private ExecutorService executorService = Executors.newFixedThreadPool(1);

    @Test
    public void testPrintMatchedStringsIsFalse() throws Exception {
        Log log = new Log();
        log.setDisplayName("TestLog");
        log.setLogDirectory("src/test/resources/");
        log.setLogName("test-log-1.log");

        SearchString searchString = new SearchString();
        searchString.setCaseSensitive(false);
        searchString.setMatchExactString(true);
        searchString.setPattern("debug");
        searchString.setDisplayName("Debug");
        searchString.setPrintMatchedString(false);
        searchString.setSendEventToController(false);

        SearchString searchString1 = new SearchString();
        searchString1.setCaseSensitive(false);
        searchString1.setMatchExactString(true);
        searchString1.setPattern("info");
        searchString1.setDisplayName("Info");
        searchString1.setPrintMatchedString(false);
        searchString1.setSendEventToController(false);

        SearchString searchString2 = new SearchString();
        searchString2.setCaseSensitive(false);
        searchString2.setMatchExactString(true);
        searchString2.setPattern("error");
        searchString2.setDisplayName("Error");
        searchString2.setPrintMatchedString(false);
        searchString2.setSendEventToController(false);

        log.setSearchStrings(Lists.newArrayList(searchString, searchString1, searchString2));

        Map<Pattern, String> replacers = new HashMap<Pattern, String>();

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + log.getLogName());
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);

        classUnderTest = new LogMonitorTask(mockFilePointerProcessor, log, replacers, executorService);

        LogMetrics result = classUnderTest.call();
        assertEquals(log.getSearchStrings().size() + 1, result.getMetrics().size());

        assertEquals(13, result.getMetrics().get("TestLog|Search String|Debug|Occurrences").intValue());
        assertEquals(24, result.getMetrics().get("TestLog|Search String|Info|Occurrences").intValue());
        assertEquals(7, result.getMetrics().get("TestLog|Search String|Error|Occurrences").intValue());

        assertEquals(getFileSize(log.getLogDirectory(), log.getLogName()),
                result.getMetrics().get("TestLog|File size (Bytes)").intValue());
    }

    @Test
    public void testMatchExactStringIsTrue() throws Exception {
        Log log = new Log();
        log.setDisplayName("TestLog");
        log.setLogDirectory("src/test/resources/");
        log.setLogName("test-log-1.log");

        SearchString searchString = new SearchString();
        searchString.setCaseSensitive(false);
        searchString.setMatchExactString(true);
        searchString.setPattern("debug");
        searchString.setDisplayName("Debug");
        searchString.setPrintMatchedString(true);
        searchString.setSendEventToController(false);

        SearchString searchString1 = new SearchString();
        searchString1.setCaseSensitive(false);
        searchString1.setMatchExactString(true);
        searchString1.setPattern("info");
        searchString1.setDisplayName("Info");
        searchString1.setPrintMatchedString(true);
        searchString1.setSendEventToController(false);

        SearchString searchString2 = new SearchString();
        searchString2.setCaseSensitive(false);
        searchString2.setMatchExactString(true);
        searchString2.setPattern("error");
        searchString2.setDisplayName("Error");
        searchString2.setPrintMatchedString(true);
        searchString2.setSendEventToController(false);

        log.setSearchStrings(Lists.newArrayList(searchString, searchString1, searchString2));

        Map<Pattern, String> replacers = new HashMap<Pattern, String>();

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + log.getLogName());
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);

        classUnderTest = new LogMonitorTask(mockFilePointerProcessor, log, replacers, executorService);

        LogMetrics result = classUnderTest.call();
        assertEquals(log.getSearchStrings().size() + 4, result.getMetrics().size());

        assertEquals(13, result.getMetrics().get("TestLog|Search String|Debug|Matches|Debug").intValue());
        assertEquals(24, result.getMetrics().get("TestLog|Search String|Info|Matches|Info").intValue());
        assertEquals(7, result.getMetrics().get("TestLog|Search String|Error|Matches|Error").intValue());

        assertEquals(13, result.getMetrics().get("TestLog|Search String|Debug|Occurrences").intValue());
        assertEquals(24, result.getMetrics().get("TestLog|Search String|Info|Occurrences").intValue());
        assertEquals(7, result.getMetrics().get("TestLog|Search String|Error|Occurrences").intValue());

        assertEquals(getFileSize(log.getLogDirectory(), log.getLogName()),
                result.getMetrics().get("TestLog|File size (Bytes)").intValue());
    }

    @Test
    public void testRegexSpecialChars() throws Exception {
        Log log = new Log();
        log.setDisplayName("TestLog");
        log.setLogDirectory("src/test/resources/");
        log.setLogName("test-log-regex.log");

        SearchString searchString = new SearchString();
        searchString.setCaseSensitive(false);
        searchString.setMatchExactString(false);
        searchString.setPattern("<");
        searchString.setDisplayName("Pattern <");
        searchString.setPrintMatchedString(true);
        searchString.setSendEventToController(false);

        SearchString searchString1 = new SearchString();
        searchString1.setCaseSensitive(false);
        searchString1.setMatchExactString(false);
        searchString1.setPattern(">");
        searchString1.setDisplayName("Pattern >");
        searchString1.setPrintMatchedString(true);
        searchString1.setSendEventToController(false);

        SearchString searchString2 = new SearchString();
        searchString2.setCaseSensitive(false);
        searchString2.setMatchExactString(false);
        searchString2.setPattern("\\*");
        searchString2.setDisplayName("Pattern *");
        searchString2.setPrintMatchedString(true);
        searchString2.setSendEventToController(false);

        SearchString searchString3 = new SearchString();
        searchString3.setCaseSensitive(false);
        searchString3.setMatchExactString(false);
        searchString3.setPattern("\\[");
        searchString3.setDisplayName("Pattern [");
        searchString3.setPrintMatchedString(true);
        searchString3.setSendEventToController(false);


        SearchString searchString4 = new SearchString();
        searchString4.setCaseSensitive(false);
        searchString4.setMatchExactString(false);
        searchString4.setPattern("\\]");
        searchString4.setDisplayName("Pattern ]");
        searchString4.setPrintMatchedString(true);
        searchString4.setSendEventToController(false);

        SearchString searchString5 = new SearchString();
        searchString5.setCaseSensitive(false);
        searchString5.setMatchExactString(false);
        searchString5.setPattern("\\.");
        searchString5.setDisplayName("Pattern .");
        searchString5.setPrintMatchedString(true);
        searchString5.setSendEventToController(false);

        log.setSearchStrings(Lists.newArrayList(searchString, searchString1, searchString2, searchString3, searchString4, searchString5));

        Map<Pattern, String> replacers = new HashMap<Pattern, String>();

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + log.getLogName());
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);

        classUnderTest = new LogMonitorTask(mockFilePointerProcessor, log, replacers, executorService);

        LogMetrics result = classUnderTest.call();
        assertEquals(log.getSearchStrings().size() + 7, result.getMetrics().size());

        assertEquals(5, result.getMetrics().get("TestLog|Search String|Pattern <|Matches|<").intValue());
        assertEquals(6, result.getMetrics().get("TestLog|Search String|Pattern >|Matches|>").intValue());
        assertEquals(16, result.getMetrics().get("TestLog|Search String|Pattern *|Matches|*").intValue());
        assertEquals(23, result.getMetrics().get("TestLog|Search String|Pattern [|Matches|[").intValue());
        assertEquals(23, result.getMetrics().get("TestLog|Search String|Pattern ]|Matches|]").intValue());
        assertEquals(2, result.getMetrics().get("TestLog|Search String|Pattern .|Matches|.").intValue());

        assertEquals(5, result.getMetrics().get("TestLog|Search String|Pattern <|Occurrences").intValue());
        assertEquals(6, result.getMetrics().get("TestLog|Search String|Pattern >|Occurrences").intValue());
        assertEquals(16, result.getMetrics().get("TestLog|Search String|Pattern *|Occurrences").intValue());
        assertEquals(23, result.getMetrics().get("TestLog|Search String|Pattern [|Occurrences").intValue());
        assertEquals(23, result.getMetrics().get("TestLog|Search String|Pattern ]|Occurrences").intValue());
        assertEquals(2, result.getMetrics().get("TestLog|Search String|Pattern .|Occurrences").intValue());

        assertEquals(getFileSize(log.getLogDirectory(), log.getLogName()),
                result.getMetrics().get("TestLog|File size (Bytes)").intValue());
    }

    @Test
    public void testRegexWords() throws Exception {
        Log log = new Log();
        log.setDisplayName("TestLog");
        log.setLogDirectory("src/test/resources/");
        log.setLogName("test-log-regex.log");

        SearchString searchString = new SearchString();
        searchString.setCaseSensitive(false);
        searchString.setMatchExactString(false);
        searchString.setPattern("(\\s|^)m\\w+(\\s|$)");
        searchString.setDisplayName("Pattern start with M");
        searchString.setPrintMatchedString(true);
        searchString.setSendEventToController(false);

        SearchString searchString1 = new SearchString();
        searchString1.setCaseSensitive(false);
        searchString1.setMatchExactString(false);
        searchString1.setPattern("<\\w*>");
        searchString1.setDisplayName("Pattern start with <");
        searchString1.setPrintMatchedString(true);
        searchString1.setSendEventToController(false);

        SearchString searchString2 = new SearchString();
        searchString2.setCaseSensitive(false);
        searchString2.setMatchExactString(false);
        searchString2.setPattern("\\[JMX.*\\]");
        searchString2.setDisplayName("Pattern start with [JMX");
        searchString2.setPrintMatchedString(true);
        searchString2.setSendEventToController(false);

        log.setSearchStrings(Lists.newArrayList(searchString, searchString1, searchString2));

        Map<Pattern, String> replacers = new HashMap<Pattern, String>();

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + log.getLogName());
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);

        classUnderTest = new LogMonitorTask(mockFilePointerProcessor, log, replacers, executorService);

        LogMetrics result = classUnderTest.call();
        assertEquals(15, result.getMetrics().size());

        // matches (\\s|^)m\\w+(\\s|$)
        assertEquals(7, result.getMetrics().get("TestLog|Search String|Pattern start with M|Matches|Memorymetricgenerator").intValue());
        assertEquals(2, result.getMetrics().get("TestLog|Search String|Pattern start with M|Matches|Memory").intValue());
        assertEquals(2, result.getMetrics().get("TestLog|Search String|Pattern start with M|Matches|Major").intValue());
        assertEquals(1, result.getMetrics().get("TestLog|Search String|Pattern start with M|Matches|Mx").intValue());
        assertEquals(1, result.getMetrics().get("TestLog|Search String|Pattern start with M|Matches|Metric").intValue());
        assertEquals(2, result.getMetrics().get("TestLog|Search String|Pattern start with M|Matches|Minor").intValue());
        assertEquals(3, result.getMetrics().get("TestLog|Search String|Pattern start with M|Matches|Metrics").intValue());
        assertEquals(1, result.getMetrics().get("TestLog|Search String|Pattern start with M|Matches|Mbean").intValue());

        // matches <\\w*>
        assertEquals(2, result.getMetrics().get("TestLog|Search String|Pattern start with <|Matches|<this>").intValue());
        assertEquals(3, result.getMetrics().get("TestLog|Search String|Pattern start with <|Matches|<again>").intValue());

        // matches \\[JMX.*\\]
        assertEquals(1, result.getMetrics().get("TestLog|Search String|Pattern start with [JMX|Matches|[jmxservice]").intValue());

        assertEquals(getFileSize(log.getLogDirectory(), log.getLogName()),
                result.getMetrics().get("TestLog|File size (Bytes)").intValue());
    }

    @Test
    public void testLogFileUpdatedWithMoreLogs() throws Exception {
        String originalFilePath = this.getClass().getClassLoader().getResource("test-log-1.log").getPath();

        String testFilename = "active-test-log.log";
        String testFilepath = String.format("%s%s%s", getTargetDir().getPath(), File.separator, testFilename);
        copyFile(originalFilePath, testFilepath);

        Log log = new Log();
        log.setDisplayName("TestLog");
        log.setLogDirectory(getTargetDir().getPath());
        log.setLogName(testFilename);

        SearchString searchString = new SearchString();
        searchString.setCaseSensitive(false);
        searchString.setMatchExactString(true);
        searchString.setPattern("debug");
        searchString.setDisplayName("Debug");
        searchString.setPrintMatchedString(true);
        searchString.setSendEventToController(false);

        SearchString searchString1 = new SearchString();
        searchString1.setCaseSensitive(false);
        searchString1.setMatchExactString(true);
        searchString1.setPattern("info");
        searchString1.setDisplayName("Info");
        searchString1.setPrintMatchedString(true);
        searchString1.setSendEventToController(false);

        SearchString searchString2 = new SearchString();
        searchString2.setCaseSensitive(false);
        searchString2.setMatchExactString(true);
        searchString2.setPattern("error");
        searchString2.setDisplayName("Error");
        searchString2.setPrintMatchedString(true);
        searchString2.setSendEventToController(false);

        log.setSearchStrings(Lists.newArrayList(searchString, searchString1, searchString2));

        Map<Pattern, String> replacers = new HashMap<Pattern, String>();

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + File.separator + log.getLogName());
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);

        classUnderTest = new LogMonitorTask(mockFilePointerProcessor, log, replacers, executorService);

        LogMetrics result = classUnderTest.call();
        assertEquals(7, result.getMetrics().size());

        assertEquals(13, result.getMetrics().get("TestLog|Search String|Debug|Matches|Debug").intValue());
        assertEquals(24, result.getMetrics().get("TestLog|Search String|Info|Matches|Info").intValue());
        assertEquals(7, result.getMetrics().get("TestLog|Search String|Error|Matches|Error").intValue());

        assertEquals(13, result.getMetrics().get("TestLog|Search String|Debug|Occurrences").intValue());
        assertEquals(24, result.getMetrics().get("TestLog|Search String|Info|Occurrences").intValue());
        assertEquals(7, result.getMetrics().get("TestLog|Search String|Error|Occurrences").intValue());

        long filesize = getFileSize(log.getLogDirectory(), log.getLogName());
        assertEquals(filesize, result.getMetrics().get("TestLog|File size (Bytes)").intValue());

        FilePointer filePointerAfterCurrentRun = LogMonitorUtil.getLatestFilePointer(result.getFilePointers());
        Mockito.verify(mockFilePointerProcessor, times(1)).updateFilePointer(filePointerAfterCurrentRun.getFilename(),
                filePointerAfterCurrentRun.getFilename(), filePointerAfterCurrentRun.getLastReadPosition(), filePointerAfterCurrentRun.getFileCreationTime());

        // simulate our filepointer was updated
        filePointer.updateLastReadPosition(filesize);
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString()))
                .thenReturn(filePointer);

        // perform the update
        List<String> logsToAdd = Arrays.asList("",
                new Date() + "	DEBUG	This is the first line",
                new Date() + "	INFO	This is the second line",
                new Date() + "	INFO	This is the third line",
                new Date() + "	DEBUG	This is the fourth line",
                new Date() + "	DEBUG	This is the fifth line");

        updateLogFile(testFilepath, logsToAdd, true);

        result = classUnderTest.call();
        assertEquals(6, result.getMetrics().size());

        assertEquals(3, result.getMetrics().get("TestLog|Search String|Debug|Matches|Debug").intValue());
        assertEquals(2, result.getMetrics().get("TestLog|Search String|Info|Matches|Info").intValue());

        assertEquals(2, result.getMetrics().get("TestLog|Search String|Info|Occurrences").intValue());
        assertEquals(3, result.getMetrics().get("TestLog|Search String|Debug|Occurrences").intValue());
        assertEquals(0, result.getMetrics().get("TestLog|Search String|Error|Occurrences").intValue());

        filePointerAfterCurrentRun = LogMonitorUtil.getLatestFilePointer(result.getFilePointers());
        Mockito.verify(mockFilePointerProcessor, times(1)).updateFilePointer(filePointerAfterCurrentRun.getFilename(),
                filePointerAfterCurrentRun.getFilename(), filePointerAfterCurrentRun.getLastReadPosition(), filePointerAfterCurrentRun.getFileCreationTime());
    }

    @Test
    public void testLogRolledOverTimeStamp() throws Exception {
        String dynamicLog1 = this.getClass().getClassLoader().getResource("dynamic-log-1.log").getPath();

        String testFilename = "active-dynamic-log-1.log";
        String testFilepath = String.format("%s%s%s", getTargetDir().getPath(), File.separator, testFilename);
        copyFile(dynamicLog1, testFilepath);

        Log log = new Log();
        log.setLogDirectory(getTargetDir().getPath());
        log.setLogName("active-dynamic-*");

        SearchString searchString = new SearchString();
        searchString.setCaseSensitive(false);
        searchString.setMatchExactString(true);
        searchString.setPattern("debug");
        searchString.setDisplayName("Debug");
        searchString.setPrintMatchedString(false);
        searchString.setSendEventToController(false);

        SearchString searchString1 = new SearchString();
        searchString1.setCaseSensitive(false);
        searchString1.setMatchExactString(true);
        searchString1.setPattern("error");
        searchString1.setDisplayName("Error");
        searchString1.setPrintMatchedString(false);
        searchString1.setSendEventToController(false);

        log.setSearchStrings(Lists.newArrayList(searchString, searchString1));

        Map<Pattern, String> replacers = new HashMap<Pattern, String>();

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + File.separator + testFilename);
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);

        classUnderTest = new LogMonitorTask(mockFilePointerProcessor, log, replacers, executorService);

        LogMetrics result = classUnderTest.call();
        assertEquals(3, result.getMetrics().size());

        assertEquals(3, result.getMetrics().get("active-dynamic-*|Search String|Debug|Occurrences").intValue());
        assertEquals(0, result.getMetrics().get("active-dynamic-*|Search String|Error|Occurrences").intValue());

        long filesize = getFileSize(log.getLogDirectory(), testFilename);
        assertEquals(filesize, result.getMetrics().get("active-dynamic-*|File size (Bytes)").intValue());

        // simulate our filepointer was updated
        filePointer.updateLastReadPosition(filesize);
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString()))
                .thenReturn(filePointer);

        List<String> logsToAdd = Lists.newArrayList();
        for (int i = 0; i < 100; i++) {
            logsToAdd.add(new Date() + "	DEBUG	Statement " + i + "\n");
        }

        updateLogFile(testFilepath, logsToAdd, true);

        // simulate new file created with different name
        Thread.sleep(1000);
        String dynamicLog2 = this.getClass().getClassLoader().getResource("dynamic-log-2.log").getPath();

        testFilename = "active-dynamic-log-2.log";
        testFilepath = String.format("%s%s%s", getTargetDir().getPath(), File.separator, testFilename);
        copyFile(dynamicLog2, testFilepath);

        // simulate another file created with different name
        Thread.sleep(1000);
        String dynamicLog3 = this.getClass().getClassLoader().getResource("dynamic-log-3.log").getPath();
        testFilename = "active-dynamic-log-3.log";
        testFilepath = String.format("%s%s%s", getTargetDir().getPath(), File.separator, testFilename);
        copyFile(dynamicLog3, testFilepath);
        logsToAdd.clear();
        for (int i = 0; i < 100; i++) {
            logsToAdd.add(new Date() + "	ERROR	Statement " + i + "\n");
        }

        updateLogFile(testFilepath, logsToAdd, true);
        result = classUnderTest.call();
        assertEquals(3, result.getMetrics().size());
        assertEquals(107, result.getMetrics().get("active-dynamic-*|Search String|Error|Occurrences").intValue());
        assertEquals(103, result.getMetrics().get("active-dynamic-*|Search String|Debug|Occurrences").intValue());
    }

    @Test
    public void testFilePointerHasLatestTimeStampAfterRolloverExecution() throws Exception {
        String dynamicLog1 = this.getClass().getClassLoader().getResource("dynamic-log-1.log").getPath();

        String testFilename = "active-dynamic-log-1.log";
        String testFilepath = String.format("%s%s%s", getTargetDir().getPath(), File.separator, testFilename);
        copyFile(dynamicLog1, testFilepath);

        Log log = new Log();
        log.setLogDirectory(getTargetDir().getPath());
        log.setLogName("active-dynamic-*");

        SearchString searchString = new SearchString();
        searchString.setCaseSensitive(false);
        searchString.setMatchExactString(true);
        searchString.setPattern("debug");
        searchString.setDisplayName("Debug");
        searchString.setPrintMatchedString(false);
        searchString.setSendEventToController(false);

        SearchString searchString1 = new SearchString();
        searchString1.setCaseSensitive(false);
        searchString1.setMatchExactString(true);
        searchString1.setPattern("error");
        searchString1.setDisplayName("Error");
        searchString1.setPrintMatchedString(false);
        searchString1.setSendEventToController(false);

        log.setSearchStrings(Lists.newArrayList(searchString, searchString1));

        Map<Pattern, String> replacers = new HashMap<Pattern, String>();

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + File.separator + testFilename);
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);
        classUnderTest = new LogMonitorTask(mockFilePointerProcessor, log, replacers, executorService);

        LogMetrics result = classUnderTest.call();
        long filesize = getFileSize(log.getLogDirectory(), testFilename);
        FilePointer latestFilePointer = LogMonitorUtil.getLatestFilePointer(result.getFilePointers());
        Mockito.verify(mockFilePointerProcessor, times(1))
                .updateFilePointer("./target/active-dynamic-*",
                latestFilePointer.getFilename(), latestFilePointer.getLastReadPosition(), latestFilePointer.getFileCreationTime());

        // simulate our filepointer was updated
        filePointer.updateLastReadPosition(filesize);
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString()))
                .thenReturn(filePointer);

        List<String> logsToAdd = Lists.newArrayList();
        for (int i = 0; i < 100; i++) {
            logsToAdd.add(new Date() + "	DEBUG	Statement " + i + "\n");
        }

        updateLogFile(testFilepath, logsToAdd, true);

        // simulate new file created with different name
        Thread.sleep(1000);
        String dynamicLog2 = this.getClass().getClassLoader().getResource("dynamic-log-2.log").getPath();

        testFilename = "active-dynamic-log-2.log";
        testFilepath = String.format("%s%s%s", getTargetDir().getPath(), File.separator, testFilename);
        copyFile(dynamicLog2, testFilepath);

        // simulate another file created with different name
        Thread.sleep(1000);
        String dynamicLog3 = this.getClass().getClassLoader().getResource("dynamic-log-3.log").getPath();
        testFilename = "active-dynamic-log-3.log";
        testFilepath = String.format("%s%s%s", getTargetDir().getPath(), File.separator, testFilename);
        copyFile(dynamicLog3, testFilepath);
        logsToAdd.clear();
        for (int i = 0; i < 100; i++) {
            logsToAdd.add(new Date() + "	ERROR	Statement " + i + "\n");
        }

        updateLogFile(testFilepath, logsToAdd, true);
        result = classUnderTest.call();

        latestFilePointer = LogMonitorUtil.getLatestFilePointer(result.getFilePointers());
        Mockito.verify(mockFilePointerProcessor, times(1)).updateFilePointer("./target/active-dynamic-*",
                latestFilePointer.getFilename(), latestFilePointer.getLastReadPosition(), latestFilePointer.getFileCreationTime());
    }

    private long getFileSize(String logDir, String logName) throws Exception {
        String fullPath = String.format("%s%s%s", logDir, File.separator, logName);
        RandomAccessFile file = new RandomAccessFile(fullPath, "r");
        long fileSize = file.length();
        file.close();
        return fileSize;
    }

    private void copyFile(String sourceFilePath, String destFilePath) throws Exception {
        FileChannel sourceChannel = null;
        FileChannel destChannel = null;

        try {
            sourceChannel = new FileInputStream(new File(sourceFilePath)).getChannel();
            destChannel = new FileOutputStream(new File(destFilePath)).getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());

        } finally {
            sourceChannel.close();
            destChannel.close();
        }
    }

    private void updateLogFile(String filepath, List<String> stringList, boolean append) throws Exception {
        File file = new File(filepath);
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(file, append);
            String output = StringUtils.join(stringList, System.getProperty("line.separator"));
            fileWriter.write(output);

        } finally {
            fileWriter.close();
        }
    }

    private File getTargetDir() {
        return new File("./target");
    }
}

