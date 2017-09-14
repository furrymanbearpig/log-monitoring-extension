package com.appdynamics.extensions.logmonitor;

import java.util.regex.Pattern;

/**
 * @author Satish Muddam
 */
public class SearchPattern {

    private String displayName;
    private Pattern pattern;
    private Boolean caseSensitive;
    private Boolean printMatchedString;

    public SearchPattern(String displayName, Pattern pattern, Boolean caseSensitive, Boolean printMatchedString) {
        this.displayName = displayName;
        this.pattern = pattern;
        this.caseSensitive = caseSensitive;
        this.printMatchedString = printMatchedString;
    }

    public String getDisplayName() {
        return displayName;
    }
    public Boolean getCaseSensitive() {
        return caseSensitive;
    }
    public Pattern getPattern() {
        return pattern;
    }
    public Boolean getPrintMatchedString() { return printMatchedString; }

}
