package com.appdynamics.extensions.logmonitor;


import com.appdynamics.extensions.conf.processor.ConfigProcessor;
import com.appdynamics.extensions.eventsservice.EventsServiceDataManager;
import com.appdynamics.extensions.yml.YmlReader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class EventsServiceIT {

    private String globalAccountName, eventsApiKey;
    private EventsServiceDataManager eventsManager;


    @Before
    public void setup() throws Exception {
        File configFile = new File("src/integration-test/resources/conf/config.yml");
        Map<String, ?> config = YmlReader.readFromFileAsMap(configFile);
        config = ConfigProcessor.process(config);
        Map<String, Object> eventsServiceParameters = (Map)config.get("eventsServiceParameters");
        Runtime.getRuntime().exec("chmod 755 src/integration-test/resources/conf/apikeys.sh");
        ProcessBuilder pb = new ProcessBuilder("src/integration-test/resources/conf/apikeys.sh");
        Process process = pb.start();
        InputStream is = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        globalAccountName = reader.readLine();
        eventsApiKey = reader.readLine();
        eventsServiceParameters.put("globalAccountName", globalAccountName);
        eventsServiceParameters.put("eventsApiKey",eventsApiKey);
        eventsManager = new EventsServiceDataManager(eventsServiceParameters);
    }

    @Test
    public void testWhetherSchemaIsCreated() {
        String schemaBody = eventsManager.retrieveSchema("logschema");
        Assert.assertTrue(schemaBody.contains("logDisplayName"));
        Assert.assertTrue(schemaBody.contains("searchPattern"));
        Assert.assertTrue(schemaBody.contains("searchPatternDisplayName"));
        Assert.assertTrue(schemaBody.contains("logMatch"));
    }

    @Test
    public void testWhetherEventsArePublished() {
        Assert.assertTrue(eventsManager.querySchema("select logDisplayName from logSchema").contains("results"));
    }
}
