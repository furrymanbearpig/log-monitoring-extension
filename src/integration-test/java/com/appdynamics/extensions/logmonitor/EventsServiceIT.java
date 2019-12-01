package com.appdynamics.extensions.logmonitor;


import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.appdynamics.extensions.eventsservice.utils.Constants.*;

public class EventsServiceIT {

    private HttpHost httpHost;
    private CloseableHttpClient httpClient;
    private String globalAccountName;
    private String eventsApiKey;

    @Before
    public void setup() throws Exception {
        Runtime.getRuntime().exec("chmod 755 src/integration-test/resources/conf/apikeys.sh");
        ProcessBuilder pb = new ProcessBuilder("src/integration-test/resources/conf/apikeys.sh");
        Process process = pb.start();
        InputStream is = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        globalAccountName = reader.readLine();
        eventsApiKey = reader.readLine();
        httpHost = new HttpHost("localhost", 9080);
        httpClient = HttpClientBuilder.create().build();
    }

    @Test
    public void testWhetherSchemaIsCreated() throws Exception {
        HttpGet httpGet = new HttpGet(httpHost.toURI() + SCHEMA_PATH + "logschema");
        httpGet.setHeader(ACCOUNT_NAME_HEADER, globalAccountName);
        httpGet.setHeader(API_KEY_HEADER, eventsApiKey);
        httpGet.setHeader(ACCEPT_HEADER, ACCEPTED_CONTENT_TYPE);
        CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
        Assert.assertEquals(200, httpResponse.getStatusLine().getStatusCode());
        String schemaBody = EntityUtils.toString(httpResponse.getEntity());
        Assert.assertTrue(schemaBody.contains("logDisplayName"));
        Assert.assertTrue(schemaBody.contains("searchPattern"));
        Assert.assertTrue(schemaBody.contains("searchPatternDisplayName"));
        Assert.assertTrue(schemaBody.contains("logMatch"));
        httpResponse.close();
    }

    @Test
    public void testWhetherEventsArePublished() {

    }

}

