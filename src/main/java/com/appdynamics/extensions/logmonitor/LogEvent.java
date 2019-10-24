package com.appdynamics.extensions.logmonitor;

public class LogEvent {
    private String logDisplayName;

    public String getLogDisplayName() {
        return logDisplayName;
    }

    public void setLogDisplayName(String logDisplayName) {
        this.logDisplayName = logDisplayName;
    }

    public String getSearchPattern() {
        return searchPattern;
    }

    public void setSearchPattern(String searchPattern) {
        this.searchPattern = searchPattern;
    }

    public String getSearchPatternDisplayName() {
        return searchPatternDisplayName;
    }

    public void setSearchPatternDisplayName(String searchPatternDisplayName) {
        this.searchPatternDisplayName = searchPatternDisplayName;
    }

    public String getLogMatch() {
        return logMatch;
    }

    public void setLogMatch(String logMatch) {
        this.logMatch = logMatch;
    }

    private String searchPattern;
    private String searchPatternDisplayName;
    private String logMatch;

}
