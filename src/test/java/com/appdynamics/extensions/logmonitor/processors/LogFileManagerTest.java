/*
 *  Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.processors;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.config.SearchString;
import com.appdynamics.extensions.logmonitor.metrics.LogMetrics;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class LogFileManagerTest {
    private LogFileManager classUnderTest;
    private FilePointerProcessor mockFilePointerProcessor = Mockito.mock(FilePointerProcessor.class);

    @Test
    public void testProcessorWhenPrintMatchedStringIsFalse() throws Exception {
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

        SearchString searchString1 = new SearchString();
        searchString1.setCaseSensitive(false);
        searchString1.setMatchExactString(true);
        searchString1.setPattern("info");
        searchString1.setDisplayName("Info");
        searchString1.setPrintMatchedString(false);

        SearchString searchString2 = new SearchString();
        searchString2.setCaseSensitive(false);
        searchString2.setMatchExactString(true);
        searchString2.setPattern("error");
        searchString2.setDisplayName("Error");
        searchString2.setPrintMatchedString(false);

        log.setSearchStrings(Lists.newArrayList(searchString, searchString1, searchString2));

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + log.getLogName());
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);

        MonitorContextConfiguration monitorContextConfiguration = new MonitorContextConfiguration("Log Monitor",
                "Custom Metrics|Log Monitor|", Mockito.mock(File.class), Mockito.mock(AMonitorJob.class));
        monitorContextConfiguration.setConfigYml("src/test/resources/conf/config.yaml");

        classUnderTest = new LogFileManager(mockFilePointerProcessor, log, monitorContextConfiguration);

        LogMetrics result = classUnderTest.getLogMetrics();

        assertEquals("13", result.getRawMetricData().get("TestLog|Search String|Debug|Occurrences").getMetricValue());
        assertEquals("24", result.getRawMetricData().get("TestLog|Search String|Info|Occurrences").getMetricValue());
        assertEquals("7", result.getRawMetricData().get("TestLog|Search String|Error|Occurrences").getMetricValue());
        assertEquals(getFileSize(log.getLogDirectory(), log.getLogName()),
                result.getRawMetricData().get("TestLog|File size (Bytes)").getMetricValue());
    }

    @Test
    public void testProcessorWhenPrintMatchedStringIsTrue() throws Exception {
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

        SearchString searchString1 = new SearchString();
        searchString1.setCaseSensitive(false);
        searchString1.setMatchExactString(true);
        searchString1.setPattern("info");
        searchString1.setDisplayName("Info");
        searchString1.setPrintMatchedString(true);

        SearchString searchString2 = new SearchString();
        searchString2.setCaseSensitive(false);
        searchString2.setMatchExactString(true);
        searchString2.setPattern("error");
        searchString2.setDisplayName("Error");
        searchString2.setPrintMatchedString(true);

        log.setSearchStrings(Lists.newArrayList(searchString, searchString1, searchString2));

        FilePointer filePointer = new FilePointer();
        filePointer.setFilename(log.getLogDirectory() + log.getLogName());
        when(mockFilePointerProcessor.getFilePointer(anyString(), anyString())).thenReturn(filePointer);

        MonitorContextConfiguration monitorContextConfiguration = new MonitorContextConfiguration("Log Monitor",
                "Custom Metrics|Log Monitor|", Mockito.mock(File.class), Mockito.mock(AMonitorJob.class));
        monitorContextConfiguration.setConfigYml("src/test/resources/conf/config.yaml");

        classUnderTest = new LogFileManager(mockFilePointerProcessor, log, monitorContextConfiguration);

        LogMetrics result = classUnderTest.getLogMetrics();

        assertEquals("13", result.getRawMetricData().get("TestLog|Search String|Debug|Occurrences").getMetricValue());
        assertEquals("24", result.getRawMetricData().get("TestLog|Search String|Info|Occurrences").getMetricValue());
        assertEquals("7", result.getRawMetricData().get("TestLog|Search String|Error|Occurrences").getMetricValue());

        assertEquals("13", result.getRawMetricData().get("TestLog|Search String|Debug|Matches|Debug").getMetricValue());
        assertEquals("24", result.getRawMetricData().get("TestLog|Search String|Info|Matches|Info").getMetricValue());
        assertEquals("7", result.getRawMetricData().get("TestLog|Search String|Error|Matches|Error").getMetricValue());

        assertEquals(getFileSize(log.getLogDirectory(), log.getLogName()),
                result.getRawMetricData().get("TestLog|File size (Bytes)").getMetricValue());
    }


    private String getFileSize(String logDir, String logName) throws Exception {
        String fullPath = String.format("%s%s%s", logDir, File.separator, logName);
        RandomAccessFile file = new RandomAccessFile(fullPath, "r");
        long fileSize = file.length();
        file.close();
        return String.valueOf(fileSize);
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
