package com.appdynamics.extensions.logmonitor.config;

/**
 * Created by aditya.jagtiani on 9/15/17.
 */
public class EventParameters {
    public String getCustomEventType() {
        return customEventType;
    }

    public void setCustomEventType(String customEventType) {
        this.customEventType = customEventType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summaryMessage) {
        this.summary = summary;
    }

    public String getApplicationID() {
        return applicationID;
    }

    public void setApplicationID(String applicationID) {
        this.applicationID = applicationID;
    }

    // TODO hardcode type = "CUSTOM" and property name as the pattern name and property values as the actual match along with the metric path.
    private String customEventType;
    private String severity;
    private String summary;
    private String applicationID;
}
