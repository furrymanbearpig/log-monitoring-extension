package com.appdynamics.extensions.logmonitor;

import com.appdynamics.extensions.controller.apiservices.MetricAPIService;
import com.appdynamics.extensions.util.JsonUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.Assert;
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
        if(metricAPIService != null) {
            JsonNode jsonNode = metricAPIService.getMetricData("","Server%20&%20Infrastructure%20Monitoring/" +
                    "metric-data?metric-path=Application%20Infrastructure%20Performance%7CRoot%7CCustom%20Metrics%7C" +
                    "Custom%20Metrics%7CLog%20Monitor%7CMachine%20Agent%20Logs%7CSearch%20String%7CLog%20Monitor%20Task%7COccurrences&time-range-type=BEFORE_NOW&duration-in-mins=60");
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
}
