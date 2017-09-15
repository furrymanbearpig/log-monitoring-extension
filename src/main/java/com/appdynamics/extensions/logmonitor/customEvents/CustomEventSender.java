package com.appdynamics.extensions.logmonitor.customEvents;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;
import java.util.List;

/**
 * Created by aditya.jagtiani on 9/15/17.
 */
public class CustomEventSender {

    public static void postCustomEvent(List<Request> requests, OkHttpClient httpClient) throws IOException {
        for (Request request : requests) {
            httpClient.newCall(request).execute();
        }
    }
}
