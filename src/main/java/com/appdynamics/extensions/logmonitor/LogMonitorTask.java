/*
 *  Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.processors.FilePointerProcessor;
import com.appdynamics.extensions.logmonitor.processors.LogFileManager;
import com.appdynamics.extensions.metrics.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.appdynamics.extensions.logmonitor.LogMonitor.metrics;
import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.getFinalMetricList;
import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.resetRegisteredMetricOccurrences;

/**
 * @author Aditya Jagtiani
 */

public class LogMonitorTask implements AMonitorTaskRunnable {
    private static Logger LOGGER = LoggerFactory.getLogger(LogMonitorTask.class);
    private MetricWriteHelper metricWriteHelper;
    private MonitorContextConfiguration monitorContextConfiguration;
    private Log log;
    private FilePointerProcessor filePointerProcessor;

    public LogMonitorTask(MonitorContextConfiguration monitorContextConfiguration, MetricWriteHelper metricWriteHelper,
                          Log log, FilePointerProcessor filePointerProcessor) {
        this.monitorContextConfiguration = monitorContextConfiguration;
        this.metricWriteHelper = metricWriteHelper;
        this.log = log;
        this.filePointerProcessor = filePointerProcessor;
    }

    public void run() {
        try {
            populateAndPrintMetrics();
        } catch (Exception ex) {
            LOGGER.error("Log monitoring task failed for Log: " + log.getDisplayName(), ex);
        }
    }

    public void onTaskComplete() {
        LOGGER.info("Completed the Log Monitoring task for log : " + log.getDisplayName());
        resetRegisteredMetricOccurrences(metrics);
    }

    private void populateAndPrintMetrics() throws Exception {
        LogFileManager logFileManager = new LogFileManager(filePointerProcessor, log, monitorContextConfiguration);
        logFileManager.processLogMetrics();
        metricWriteHelper.transformAndPrintMetrics(getFinalMetricList(metrics));
        filePointerProcessor.updateFilePointerFile();
    }
}