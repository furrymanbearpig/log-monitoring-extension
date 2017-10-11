package com.appdynamics.extensions.logmonitor.config;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.List;

/**
 * @author Florencio Sarmiento
 */
public class Configuration {

    private String metricPrefix;

    private List<Log> logs;

    private List<MetricCharacterReplacer> metricCharacterReplacer;

    private int noOfThreads;

    public int getThreadTimeOut() {
        return threadTimeOut;
    }

    public void setThreadTimeOut(int threadTimeOut) {
        this.threadTimeOut = threadTimeOut;
    }

    private int threadTimeOut;

    public ControllerInfo getControllerInfo() {
        return controllerInfo;
    }

    public void setControllerInfo(ControllerInfo controllerInfo) {
        this.controllerInfo = controllerInfo;
    }

    public EventParameters getEventParameters() {
        return eventParameters;
    }

    public void setEventParameters(EventParameters eventParameters) {
        this.eventParameters = eventParameters;
    }

    private ControllerInfo controllerInfo;

    private EventParameters eventParameters;

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }

    public List<Log> getLogs() {
        return logs;
    }

    public void setLogs(List<Log> logs) {
        this.logs = logs;
    }

    public List<MetricCharacterReplacer> getMetricCharacterReplacer() {
        return metricCharacterReplacer;
    }

    public void setMetricCharacterReplacer(List<MetricCharacterReplacer> metricCharacterReplacer) {
        this.metricCharacterReplacer = metricCharacterReplacer;
    }

    public int getNoOfThreads() {
        return noOfThreads;
    }

    public void setNoOfThreads(int noOfThreads) {
        this.noOfThreads = noOfThreads;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this,
                ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
