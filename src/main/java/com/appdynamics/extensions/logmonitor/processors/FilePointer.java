/*
 *  Copyright 2018. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.processors;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Aditya Jagtiani
 */
public class FilePointer {

    private volatile String filename;

    private AtomicLong lastReadPosition = new AtomicLong(0);

    private long fileCreationTime;

    String getFilename() {
        return filename;
    }

    synchronized void setFilename(String filename) {
        this.filename = filename;
    }

    AtomicLong getLastReadPosition() {
        return lastReadPosition;
    }

    synchronized void setLastReadPosition(AtomicLong lastReadPosition) {
        this.lastReadPosition = lastReadPosition;
    }

    synchronized void updateLastReadPosition(long lastReadPosition) {
        if (this.lastReadPosition == null) {
            this.lastReadPosition = new AtomicLong(lastReadPosition);
        } else {
            this.lastReadPosition.set(lastReadPosition);
        }
    }

    public long getFileCreationTime() {
        return this.fileCreationTime;
    }

    void setFileCreationTime(long fileCreationTime) {
        this.fileCreationTime = fileCreationTime;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this,
                ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
