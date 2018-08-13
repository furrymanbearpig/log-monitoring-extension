/*
 *  Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.metrics;

import com.appdynamics.extensions.logmonitor.config.FilePointer;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Aditya Jagtiani
 */
public class LogMetrics {
    private String metricPrefix;
    private Map<String, Metric> metrics = new ConcurrentHashMap<String, Metric>();
    private CopyOnWriteArrayList<FilePointer> filePointers = new CopyOnWriteArrayList<FilePointer>();

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }

    public void add(String metricName, String metricPath) {
        BigInteger value;
        if (metrics.containsKey(metricName)) {
            value = new BigInteger(metrics.get(metricName).getMetricValue()).add(BigInteger.ONE);
        } else {
            value = BigInteger.ONE;
        }
        add(metricName, new Metric(metricName, String.valueOf(value), metricPath));
    }

    public void add(String metricName, Metric metric) {
        this.metrics.put(metricName, metric);
    }

    public List<Metric> getFinalMetricList() {
        List<Metric> metrics = Lists.newArrayList();
        for (Map.Entry<String, Metric> metric : this.metrics.entrySet()) {
            metrics.add(metric.getValue());
        }
        return metrics;
    }

    public Map<String, Metric> getMetricMap() {
        return this.metrics;
    }

    public CopyOnWriteArrayList<FilePointer> getFilePointers() {
        return this.filePointers;
    }

    public void updateFilePointer(FilePointer filePointer) {
        filePointers.add(filePointer);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this,
                ToStringStyle.SHORT_PREFIX_STYLE);
    }
}

