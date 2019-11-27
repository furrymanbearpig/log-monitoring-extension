package com.appdynamics.extensions.logmonitor;

import com.appdynamics.extensions.controller.apiservices.MetricAPIService;
import org.junit.Before;
import org.junit.Test;

import static com.appdynamics.extensions.logmonitor.IntegrationTestUtils.initializeMetricAPIService;

public class MetricCheckIT {

    private MetricAPIService metricAPIService;

    @Before
    public void setup() {
        metricAPIService = initializeMetricAPIService();
    }

    @Test
    public void testMetricUpload() {
/*
        JsonNode jsonNode = null;
        if (metricAPIService != null) {
            jsonNode = metricAPIService.getMetricData("",
                    "Server%20&%20Infrastructure%20Monitoring/metric-data?metric-path=Application%20" +
                            "Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7CCassandra%7CLocal%20Cassandra%20" +
                            "Server%201%7CHeart%20Beat&time-range-type=BEFORE_NOW&duration-in-mins=60&output=JSON");
        }
        Assert.assertNotNull("Cannot connect to controller API", jsonNode);
        if (jsonNode != null) {
            JsonNode valueNode = JsonUtils.getNestedObject(jsonNode, "*", "metricValues", "*", "value");
            int heartBeat = (valueNode == null) ? 0 : valueNode.get(0).asInt();
            Assert.assertEquals("heartbeat is 0", heartBeat, 1);
        }*/
    }
}
