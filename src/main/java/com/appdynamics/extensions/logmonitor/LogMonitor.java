package com.appdynamics.extensions.logmonitor;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logmonitor.config.Log;
import com.appdynamics.extensions.logmonitor.config.MetricCharacterReplacer;
import com.appdynamics.extensions.logmonitor.processors.FilePointer;
import com.appdynamics.extensions.logmonitor.processors.FilePointerProcessor;
import com.appdynamics.extensions.logmonitor.util.LogMonitorUtil;
import com.appdynamics.extensions.util.AssertUtils;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.appdynamics.extensions.logmonitor.util.Constants.DEFAULT_METRIC_PREFIX;
import static com.appdynamics.extensions.logmonitor.util.Constants.MONITOR_NAME;

/**
 * Created by aditya.jagtiani on 3/30/18.
 */
public class LogMonitor extends ABaseMonitor {
    private static Logger logger = LoggerFactory.getLogger(LogMonitor.class);
    private MonitorContextConfiguration monitorContextConfiguration;
    private Map<String, ?> configYml = Maps.newHashMap();

    @Override
    public String getDefaultMetricPrefix() { return DEFAULT_METRIC_PREFIX; }

    @Override
    public String getMonitorName() { return MONITOR_NAME; }

    @Override
    protected void initializeMoreStuff(Map<String, String> args) {
        monitorContextConfiguration = getContextConfiguration();
        configYml = monitorContextConfiguration.getConfigYml();
    }

    @Override
    protected int getTaskCount() {
        List<Map<String, ?>> logsFromCfg = (List<Map<String, ?>>) configYml.get("logs");
        AssertUtils.assertNotNull(logsFromCfg, "Please populate the 'logs' section in the config.yml.");
        return logsFromCfg.size();
    }

    @Override
    public void doRun(TasksExecutionServiceProvider taskExecutor) {
        List<Map<String, ?>> logsFromCfg = (List<Map<String, ?>>) configYml.get("logs");
        List<Log> logsToMonitor = LogMonitorUtil.getValidLogsFromConfig(logsFromCfg);

        FilePointerProcessor filePointerProcessor = new FilePointerProcessor();

        for(Log log : logsToMonitor) {
            logger.info("Starting the Log Monitoring Task for log : " + log.getDisplayName());
            LogMonitorTask task = new LogMonitorTask(monitorContextConfiguration, taskExecutor.getMetricWriteHelper(), log, filePointerProcessor);
            taskExecutor.submit(log.getDisplayName(), task);
        }
    }
}
