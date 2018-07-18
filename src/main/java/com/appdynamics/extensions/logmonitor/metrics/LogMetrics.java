/*
 *  Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.metrics;

import com.appdynamics.extensions.logmonitor.processors.FilePointer;
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
    private Map<String, BigInteger> metrics = new ConcurrentHashMap<String, BigInteger>();
    private CopyOnWriteArrayList<FilePointer> filePointers = new CopyOnWriteArrayList<FilePointer>();

    public void add(String metricName) {
        BigInteger value = metrics.get(metricName);

        if (value != null) {
            value = value.add(BigInteger.ONE);
        } else {
            value = BigInteger.ONE;
        }

        add(metricName, value);
    }

    public void add(String metricName, BigInteger value) {
        this.metrics.put(metricName, value);
    }

    public List<Metric> getMetrics() {
        List<Metric> metrics = Lists.newArrayList();
        for(Map.Entry<String, BigInteger> metric : this.metrics.entrySet()) {
            metrics.add(new Metric(metric.getKey(), String.valueOf(metric.getValue()), ""));
        }
        return metrics;
    }

    public Map<String, BigInteger> getRawMetricData() {return this.metrics;}

    public CopyOnWriteArrayList<FilePointer> getFilePointers() {
        return this.filePointers;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this,
                ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public void updateFilePointer(FilePointer filePointer) {
        filePointers.add(filePointer);
    }

}

