/*
 *  Copyright 2019. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.logmonitor;

import com.appdynamics.extensions.controller.apiservices.CustomDashboardAPIService;
import com.appdynamics.extensions.controller.apiservices.MetricAPIService;
import com.appdynamics.extensions.util.JsonUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.appdynamics.extensions.logmonitor.IntegrationTestUtils.initializeCustomDashboardAPIService;
import static com.appdynamics.extensions.logmonitor.IntegrationTestUtils.initializeMetricAPIService;
import static com.appdynamics.extensions.logmonitor.IntegrationTestUtils.isDashboardPresent;

/**
 * @author: Aditya Jagtiani
 */

public class MetricCheckIT {

    private MetricAPIService metricAPIService;
    private CustomDashboardAPIService customDashboardAPIService;

    @Before
    public void setup() {
        metricAPIService = initializeMetricAPIService();
        customDashboardAPIService = initializeCustomDashboardAPIService();
    }

    @Test
    public void testMetricUpload() {
        if(metricAPIService != null) {
            JsonNode jsonNode = metricAPIService.getMetricData("","Server%20&%20Infrastructure%" +
                    "20Monitoring/metric-data?metric-path=Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics" +
                    "%7CLog%20Monitor%7CMachine%20Agent%20Logs%7CSearch%20String%7CLog%20Monitor%20Task%7COccurrences&time-range-" +
                    "type=BEFORE_NOW&duration-in-mins=5");
            if (jsonNode != null) {
                JsonNode valueNode = JsonUtils.getNestedObject(jsonNode, "*", "metricValues", "*", "value");
                int occurrences = (valueNode == null) ? 0 : valueNode.get(0).asInt();
                Assert.assertTrue(occurrences > 0);
            }
        }
        else {
            Assert.fail("Failed to connect to the Controller API");
        }
    }

    @Test
    public void checkDashboardsUploaded() {
        boolean isDashboardPresent = false;
        if (customDashboardAPIService != null) {
            JsonNode allDashboardsNode = customDashboardAPIService.getAllDashboards();
            isDashboardPresent = isDashboardPresent("Log Monitor Dashboard", allDashboardsNode);
            Assert.assertTrue(isDashboardPresent);
        } else {
            Assert.assertFalse(isDashboardPresent);
        }
    }
}
