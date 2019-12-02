package com.appdynamics.extensions.logmonitor;


import com.appdynamics.extensions.conf.processor.ConfigProcessor;
import com.appdynamics.extensions.eventsservice.EventsServiceDataManager;
import com.appdynamics.extensions.http.Http4ClientBuilder;
import com.appdynamics.extensions.yml.YmlReader;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static com.appdynamics.extensions.eventsservice.utils.Constants.*;

public class EventsServiceIT {
    private CloseableHttpClient httpClient;
    private HttpHost httpHost;
    private String globalAccountName, eventsApiKey;
    private EventsServiceDataManager eventsServiceDataManager;

    @Before
    public void setup() throws Exception {
        File configFile = new File("src/integration-test/resources/conf/config.yml");
        Map<String, ?> config = YmlReader.readFromFileAsMap(configFile);
        config = ConfigProcessor.process(config);
        Map<String, Object> eventsServiceParameters = (Map) config.get("eventsServiceParameters");
        String eventsServiceHost = (String) eventsServiceParameters.get("host");
        int eventsServicePort = (Integer) eventsServiceParameters.get("port");
/*        Runtime.getRuntime().exec("chmod 755 src/integration-test/resources" +
                "/conf/apikeys.sh");
        ProcessBuilder pb = new ProcessBuilder("src/integration-test/resources/conf/apikeys.sh");
        Process process = pb.start();
        InputStream is = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        globalAccountName = reader.readLine();
        eventsApiKey = reader.readLine();
        eventsServiceParameters.put("globalAccountName", globalAccountName);
        eventsServiceParameters.put("eventsApiKey", eventsApiKey);*/
        boolean useSSL = (Boolean) eventsServiceParameters.get("useSSL");
        httpClient = Http4ClientBuilder.getBuilder(eventsServiceParameters).build();
        httpHost = new HttpHost(eventsServiceHost, eventsServicePort, useSSL ? "https" : "http");
        eventsServiceDataManager = new EventsServiceDataManager(eventsServiceParameters);
    }

    @Test
    public void testWhetherSchemaIsCreated() {
        Assert.assertTrue(eventsServiceDataManager.retrieveSchema("logSchema").contains("logDisplayName"));
    }

    @Test
    public void testWhetherEventsArePublished() {
        Assert.assertTrue(eventsServiceDataManager.querySchema("select * from logschema").contains("results"));
    }
}
