/*
 *  Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.config;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.List;

/**
 * @author Florencio Sarmiento
 *
 */
public class Logs {

    public LogInfo[] getLogInfos() {
        return logInfos;
    }

    public void setLogInfos(LogInfo[] logInfos) {
        this.logInfos = logInfos;
    }

    private LogInfo[] logInfos;

}
