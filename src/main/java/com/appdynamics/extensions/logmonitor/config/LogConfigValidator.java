/*
 *  Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.config;

import org.apache.commons.lang.StringUtils;

/**
 * @author Florencio Sarmiento
 *
 */
public class LogConfigValidator {
	
	public static void validate(Log log) {
		if (StringUtils.isBlank(log.getLogDirectory())) {
			throw new IllegalArgumentException("Log directory must not be blank.");
		}
		
		if (StringUtils.isBlank(log.getLogName())) {
			throw new IllegalArgumentException("Log name must not be blank.");
		}
		
		if (log.getSearchStrings() == null || log.getSearchStrings().isEmpty()) {
			throw new IllegalArgumentException("You must provide at least one search string.");
		}
	}

}
