/*
 *  Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.processors.FilePointerProcessor;
import com.appdynamics.extensions.logmonitor.util.LogMonitorUtil;
import com.appdynamics.extensions.util.AssertUtils;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.logmonitor.util.Constants.DEFAULT_METRIC_PREFIX;
import static com.appdynamics.extensions.logmonitor.util.Constants.MONITOR_NAME;

/**
 * @author Aditya Jagtiani
 */

public class LogMonitor extends ABaseMonitor {
    private static Logger LOGGER = Logger.getLogger(LogMonitor.class);
    private MonitorContextConfiguration monitorContextConfiguration;
    private Map<String, ?> configYml = Maps.newHashMap();

    @Override
    public String getDefaultMetricPrefix() {
        return DEFAULT_METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return MONITOR_NAME;
    }

    @Override
    protected void initializeMoreStuff(Map<String, String> args) {
        monitorContextConfiguration = getContextConfiguration();
        configYml = monitorContextConfiguration.getConfigYml();
    }

    protected List<Map<String, ?>> getServers() {
        return new ArrayList<Map<String, ?>>() {
        };
    }


    protected int getTaskCount() {
        List<Map<String, ?>> logsFromConfig = (List<Map<String, ?>>) configYml.get("logs");
        AssertUtils.assertNotNull(logsFromConfig, "Please populate the 'logs' section in the config.yml.");
        return logsFromConfig.size();
    }

    @Override
    public void doRun(TasksExecutionServiceProvider taskExecutor) {
        List<Map<String, ?>> logsFromConfig = (List<Map<String, ?>>) configYml.get("logs");
        List<Log> logsToMonitor = LogMonitorUtil.getValidLogsFromConfig(logsFromConfig, (String) configYml.get("metricPrefix"));
        FilePointerProcessor filePointerProcessor = new FilePointerProcessor();
        for (Log log : logsToMonitor) {
            LOGGER.info("Starting the Log Monitoring Task for log : " + log.getDisplayName());
            LogMonitorTask task = new LogMonitorTask(monitorContextConfiguration, taskExecutor.getMetricWriteHelper(),
                    log, filePointerProcessor);
            taskExecutor.submit(log.getDisplayName(), task);
        }
    }

    public static void main(String[] args) throws TaskExecutionException {
        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
        ca.setThreshold(Level
                .DEBUG);
        org.apache.log4j.Logger.getRootLogger().addAppender(ca);


/*FileAppender fa = new FileAppender(new PatternLayout("%-5p [%t]: %m%n"), "cache.log");
fa.setThreshold(Level.DEBUG);
LOGGER.getRootLogger().addAppender(fa);*/

        LogMonitor monitor = new LogMonitor();
        Map<String, String> taskArgs = new HashMap<String, String>();
        taskArgs.put("config-file", "/Users/aj89/repos/appdynamics/extensions/log-monitoring-extension/src/integration-test/resources/conf/config.yml");
        monitor.execute(taskArgs, null);
    }
}