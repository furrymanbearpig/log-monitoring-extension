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

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.logmonitor.util.LogMonitorUtil.processRawMetrics;

/**
 * Created by aditya.jagtiani on 3/30/18.
 */
public class LogMonitorTask implements AMonitorTaskRunnable {
    private static Logger logger = LoggerFactory.getLogger(LogMonitorTask.class);
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
            logger.error("Log monitoring task failed for Log: " + log.getDisplayName(), ex);
        }
    }

    public void onTaskComplete() {
        logger.info("Completed the Log Monitoring task for log : " + log.getDisplayName());
    }

    private void populateAndPrintMetrics() {
        LogFileManager logFileManager = new LogFileManager(filePointerProcessor, log, monitorContextConfiguration);
        List<Metric> allLogMetrics = Lists.newArrayList();
        allLogMetrics.addAll(processRawMetrics(logFileManager.getLogMetrics().getMetrics()));
        metricWriteHelper.transformAndPrintMetrics(allLogMetrics);
        logger.debug("Successfully completed the Log Monitoring task for log {}", log.getLogName());
    }
}
