package com.appdynamics.extensions.logmonitor;


import com.appdynamics.extensions.controller.apiservices.CustomDashboardAPIService;
import com.appdynamics.extensions.controller.apiservices.MetricAPIService;
import com.appdynamics.extensions.util.JsonUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.appdynamics.extensions.logmonitor.IntegrationTestUtils.initializeMetricAPIService;

public class EventsServiceIT {


    @After
    public void tearDown() {
        //todo: shutdown client
    }


}
