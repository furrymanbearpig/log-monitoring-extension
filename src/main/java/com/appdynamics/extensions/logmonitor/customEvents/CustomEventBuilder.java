package com.appdynamics.extensions.logmonitor.customEvents;

import com.appdynamics.extensions.logmonitor.config.ControllerInfo;
import com.appdynamics.extensions.logmonitor.config.EventParameters;
import com.google.common.collect.Lists;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.util.List;

import static com.appdynamics.extensions.logmonitor.Constants.CONTROLLER_EVENTS_ENDPOINT;
import static com.appdynamics.extensions.logmonitor.Constants.EVENT_REQUEST_HEADER;
import static com.appdynamics.extensions.logmonitor.Constants.EVENT_TYPE;

/**
 * Created by aditya.jagtiani on 9/15/17.
 */
public class CustomEventBuilder {

    public static List<Request> eventsToBePosted = Lists.newArrayList();

    public static void createEvent(ControllerInfo controllerInfo, EventParameters eventParameters, String propertyName, String propertyValue) {
        MediaType mediaType = MediaType.parse("application/octet-stream");
        RequestBody body = RequestBody.create(mediaType, buildRequestContent(controllerInfo, eventParameters, propertyName, propertyValue));
        Request request = new Request.Builder()
                .url("http://")
                .post(body)
                .addHeader("cache-control", "no-cache")
                .build();
        eventsToBePosted.add(request);
    }

    private static String buildRequestContent(ControllerInfo controllerInfo, EventParameters eventParameters, String propertyName, String propertyValue) {
        return EVENT_REQUEST_HEADER + controllerInfo.getUsername() + "@" + controllerInfo.getAccount() + ":" + controllerInfo.getUsername() +
                "'" + controllerInfo.getControllerUrl() + CONTROLLER_EVENTS_ENDPOINT + eventParameters.getApplicationID() + "/events?severity=" + eventParameters.getSeverity() +
                "&summary=" + eventParameters.getSummary() +
                "&eventtype=" + EVENT_TYPE + "&customeventtype=" + eventParameters.getCustomEventType() +
                "&propertynames=" + propertyName + "&propertyvalues=" + propertyValue;
    }
}
