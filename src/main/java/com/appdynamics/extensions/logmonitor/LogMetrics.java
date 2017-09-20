package com.appdynamics.extensions.logmonitor;

import java.math.BigInteger;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.appdynamics.extensions.logmonitor.processors.FilePointer;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author Florencio Sarmiento
 */
public class LogMetrics {
    private Map<String, BigInteger> metrics = new ConcurrentHashMap<String, BigInteger>();
    private CopyOnWriteArrayList<FilePointer> filePointers = new CopyOnWriteArrayList<FilePointer>();
    private CopyOnWriteArrayList<URL> eventsToBePosted = new CopyOnWriteArrayList<URL>();

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

    public void addAll(Map<String, BigInteger> metrics) {
        this.metrics.putAll(metrics);
    }

    public Map<String, BigInteger> getMetrics() {
        return this.metrics;
    }

    public CopyOnWriteArrayList<FilePointer> getFilePointers() {
        return this.filePointers;
    }

    public CopyOnWriteArrayList<URL> getEventsToBePosted() { return this.eventsToBePosted;}
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this,
                ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public void updateFilePointer(FilePointer filePointer) {
        filePointers.add(filePointer);
    }

    public void updateEventsToBePosted(URL url) { eventsToBePosted.add(url); }
}
