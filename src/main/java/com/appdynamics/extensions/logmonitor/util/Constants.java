/*
 *  Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor.util;

/**
 * @author Florencio Sarmiento
 *
 */
public final class Constants {
	
	public static final String CONFIG_ARG = "config-file";
	
	public static final String FILEPOINTER_FILENAME = "filepointer.json";
	
	public static final String METRIC_PATH_SEPARATOR = "|";
	
	public static final String DEFAULT_METRIC_PATH = String.format("%s%s%s%s", "Custom Metrics", 
			METRIC_PATH_SEPARATOR, "LogMonitor", METRIC_PATH_SEPARATOR);
	
	public static final String SEARCH_STRING = "Search String";
	
	public static final String FILESIZE_METRIC_NAME = "File size (Bytes)";
	
	public static final int DEFAULT_NO_OF_THREADS = 3;

	public static final String EVENT_TYPE = "CUSTOM";

	public static final String CONTROLLER_EVENTS_ENDPOINT = "/controller/rest/applications/";

	public static final String DEFAULT_METRIC_PREFIX = "Custom Metrics|Log Monitor|";
	public static final String MONITOR_NAME = "Log Monitor";
}
