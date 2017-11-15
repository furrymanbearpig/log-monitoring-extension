# Log Monitoring Extension  

## Use Case

Use for monitoring log files to report:

- the no of occurrences of each search term provided
- filesize 

Typical usage is counting how many 'Warn' and/or 'Error' are logged. 

This extension works only with standalone machine agent.

Note: By default, the Machine agent can only send a fixed number of metrics to the controller. This extension can potentially report hundreds of metrics, so to change this limit, please follow the instructions mentioned [here](https://docs.appdynamics.com/display/PRO40/Metrics+Limits).

## Installation
1. Run 'mvn clean install' from log-monitoring-extension directory
2. Copy and unzip LogMonitor.zip from 'target' directory into \<machine_agent_dir\>/monitors/
3. Edit config.yaml file in LogMonitor/conf file and provide the required configuration (see Configuration section)
4. Restart the Machine Agent.

## Configuration

### config.yaml


| Param | Description |
| ----- | ----- |
| displayName | The display name of the log file. If not specified, logName is used by default. |
| logDirectory | The directory path where the log is located. |
| logName | The name of the log file, i.e. server.log. Supports wildcard character for filename that changes dynamically on rotation, e.g. server-*.log|
| searchStrings/displayName | Display name for this pattern |
| searchStrings/pattern | The strings to search, e.g. "debug", "info", "error". Supports regex if matchExactString is set to false. Note, this is case insensitive regardless.|
| searchStrings/matchExactString | Allowed values: **true** or **false**. Set to true if you only want to match the exact string, otherwise set to false for regex support and contains in string. |
| searchStrings/caseSensitive | Allowed values: **true** or **false**. Set to true if you want the search to be case sensitive, otherwise false |
| searchStrings/printMatchedString | Allowed values: **true** or **false**. Set to true if you wish to see the strings that were matched. Setting it to false will only show the number of matches found. |
| ----- | ----- |
| noOfThreads | The no of threads used to process multiple logs concurrently |
| metricPrefix | The path prefix for viewing metrics in the metric browser. Default value is "Custom Metrics\|LogMonitor\|" |

Below is an example config with multiple log files to monitor, one of which uses the dynamic filename and search string regex support.

<pre>
#prefix used to show metrics in all tiers (NOT RECOMMENDED)
#metricPrefix: "Custom Metrics|LogMonitor|"

# metric prefix to show metrics in one tier (HIGHLY RECOMMENDED)
# Please follow the Metric Path section of https://docs.appdynamics.com/display/PRO42/Build+a+Monitoring+Extension+Using+Java for instructions on retrieving the tier ID

metricPrefix: "Server|Component:<TIER_ID>|Custom Metrics|Log Monitor|"

logs:
  - displayName: "Machine Agent Log"
    logDirectory: "/Users/aditya.jagtiani/appdynamics/machineagent-4.1.8.12/logs/test"
    logName: "error.log"
    searchStrings:
       - displayName: "Error in uppercase"
         pattern: "ERROR"
         matchExactString: false
         caseSensitive: false
         printMatchedString: false
        #displayName Should be unique across the patterns including the case.
       - displayName: "Debug In lowercase"
         pattern: "debug"
         matchExactString: true
         caseSensitive: false
         printMatchedString: false
       - displayName: "Trace in lowercase"
         pattern: "trace"
         matchExactString: false
         caseSensitive: false
         printMatchedString: false

#Replaces characters in metric name with the specified characters.
# "replace" takes any regular expression
# "replaceWith" takes the string to replace the matched characters
metricCharacterReplacer:
    - replace: ":"
      replaceWith: ";"
    - replace: "\\|"
      replaceWith: "#"

# Number of concurrent threads
noOfThreads: 3

# Thread timeout in seconds
threadTimeOut: 60
</pre>

## Metric Path

Application Infrastructure Performance|\<Tier\>|Custom Metrics|LogMonitor|\<LogName\>|Search String|\<searchStrings displayName\>|\<Matched Term\>

Application Infrastructure Performance|\<Tier\>|Custom Metrics|LogMonitor|\<LogName\>|File size (Bytes)

## Custom Dashboard
![](https://raw.github.com/Appdynamics/log-monitoring-extension/master/LogMonitorCustomDashboard.png)

##Contributing

Always feel free to fork and contribute any changes directly here on GitHub

## Community

Find out more in the [AppSphere](http://community.appdynamics.com/t5/eXchange-Community-AppDynamics/Log-Monitoring-Extension/idi-p/8830) community.


## Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:ace-request@appdynamics.com).

