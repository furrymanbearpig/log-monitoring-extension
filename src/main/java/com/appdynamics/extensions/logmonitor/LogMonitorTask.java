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
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.logmonitor.LogMonitor.baseMetrics;
import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.getFinalMetricList;

/**
 * Created by aditya.jagtiani on 3/30/18.
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
    }

    private void populateAndPrintMetrics() throws Exception {
        LogFileManager logFileManager = new LogFileManager(filePointerProcessor, log, monitorContextConfiguration);
        List<Metric> allLogMetrics = Lists.newArrayList();
        Map<String, Metric> processedMetrics = logFileManager.getLogMetrics().getMetricMap();

        if(processedMetrics.size() <= 1) {
            allLogMetrics.addAll(getFinalMetricList(baseMetrics));
        }

        else {
            allLogMetrics.addAll(getFinalMetricList(processedMetrics));
        }

        metricWriteHelper.transformAndPrintMetrics(allLogMetrics);
        filePointerProcessor.updateFilePointerFile();
    }
}