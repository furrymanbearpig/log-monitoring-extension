
package com.appdynamics.extensions.logmonitor;


import com.appdynamics.extensions.eventsservice.EventsServiceDataManager;
import com.appdynamics.extensions.yml.YmlReader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Map;

public class EventsServiceIT {

    private EventsServiceDataManager eventsServiceDataManager;

    @Before
    public void setup() {
        File configFile = new File("src/integration-test/resources/conf/config.yml");
        Map<String, ?> config = YmlReader.readFromFileAsMap(configFile);
        Map<String, ? super Object> eventsServiceParameters = (Map) config.get("eventsServiceParameters");
        eventsServiceDataManager = new EventsServiceDataManager(eventsServiceParameters);
    }

    @Test
    public void testWhetherSchemaIsCreated() {
        String schemaBody = eventsServiceDataManager.retrieveSchema("logSchema");
        Assert.assertTrue(schemaBody.contains("logDisplayName"));
        Assert.assertTrue(schemaBody.contains("searchPattern"));
        Assert.assertTrue(schemaBody.contains("searchPatternDisplayName"));
        Assert.assertTrue(schemaBody.contains("logMatch"));
    }

    @Test
    public void testWhetherEventsArePublished() {
        Assert.assertTrue(eventsServiceDataManager.querySchema("select logDisplayName from logSchema").contains("results"));
    }
}

