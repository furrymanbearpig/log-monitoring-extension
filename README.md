# AppDynamics Log Monitoring Extension

## Use Case
The AppDynamics Log Monitoring Extension monitors the occurrences of configured text or regular expressions in a set of log files, and the sizes of these files. The extension works seamlessly with logs that are constantly generated and rotating from time to time. 

## Prerequisites
In order to use this extension, you need a [Standalone JAVA Machine Agent](https://docs.appdynamics.com/display/PRO44/Java+Agent) or a [SIM Agent](https://docs.appdynamics.com/display/PRO44/Server+Visibility).
For more details on downloading these products, please visit https://download.appdynamics.com/.

The extension must be deployed on the same box as the one with the log files you wish to monitor.

## Installation
1. To build from source, clone this repository and run 'mvn clean install'. This will produce a LogMonitor-VERSION.zip in the target directory
Alternatively, download the latest release archive from [GitHub](https://github.com/Appdynamics/log-monitoring-extension/releases)
2. Unzip the file LogMonitor-[version].zip into `<MACHINE_AGENT_HOME>/monitors/`
3. In the newly created directory "LogMonitor", edit the config.yml to configure the parameters (See Configuration section below)
4. Restart the Machine Agent
5. In the AppDynamics Metric Browser, look for: Application Infrastructure Performance|\<Tier\>|Custom Metrics|Log Monitor

## Configuration

Configure the Log Monitoring Extension by editing the ```config.yml``` & ```monitor.xml``` files in `<MACHINE_AGENT_HOME>/monitors/LogMonitor/`.

### 1. Tier Configuration

Configure the Tier under which the metrics should be reported. This can be done by adding the Tier ID to the metric prefix. 
```metricPrefix: "Server|Component:<TIER_ID>|Custom Metrics|Log Monitor|"```

### 2. Log Configuration

This includes specifying the location of the logs and the various search strings/regular expressions you'd like to look for in the logs. 
There are multiple scenarios that can be configured in the ```logs``` section:

#### 2.1 Static Logs 

Consider a log file, ```myLog.log``` that does not rollover. To search for case sensitive matches of ```ERROR```: 

```
logs:
     - displayName: "Test Log"
       logDirectory: "/Users/XYZ/MyApplication/logs"
       logName: "myLog.log"
       encoding: ""      #Not mandatory. Supported types: UTF8, UTF16, UTF16-LE, UTF16-BE, UTF32, UTF-32LE, UTF32-BE
       searchStrings:
           #displayName Should be unique across the various patterns.
          - displayName: "Errors"
            pattern: "ERROR"
            matchExactString: true
            caseSensitive: true
            printMatchedString: true
```

#### 2.2 Dynamic Logs 

Consider a log, ```myLog.log``` that rolls over after reaching a pre-configured file size threshold to ```myLog.log.1```, ```myLog.log.2``` etc. 
To search for case sensitive matches of ```ERROR``` across the entire rollover sequence: 

```
logs:
     - displayName: "Test Log"
       logDirectory: "/Users/XYZ/MyApplication/logs"
       logName: "myLog.log*" #Use of a regex to include the entire rollover set
       searchStrings:
           #displayName Should be unique across the various patterns.
          - displayName: "Errors"
            pattern: "ERROR"
            matchExactString: true
            caseSensitive: true
            printMatchedString: true
```

#### 2.3 Common Log Scenarios

1. The ```pattern``` section under searchStrings accepts regular expressions. 
Consider a scenario where you're monitoring errors like the one below:   
```[Thread-1] 29 Apr 2014 12:31:18,680  ERROR MemoryMetricGenerator - Identified major collection bean :ConcurrentMarkSweep```

To get the complete line as a metric: 
```
searchStrings:
           #displayName Should be unique across the various patterns.
          - displayName: "Memory Metric Generator Errors"
            pattern: "(.*)ERROR(.*)"
            matchExactString: true
            caseSensitive: true
            printMatchedString: true
```

2. The extension supports various Unicode character sets that your logs may be encoded in. 
To monitor a non-UTF8 encoded file, add the encoding type under the ```encoding``` section for the log. Supported encoding types are `UTF8, UTF16, UTF16-LE, UTF16-BE, UTF32, UTF-32LE, UTF32-BE` .
```
logs:
     - displayName: "Test UTF-16 Log"
       logDirectory: "/Users/XYZ/MyApplication/UTF16Logs"
       logName: "myLogUTF16LE.log"
       encoding: "UTF16-LE"
       searchStrings:
           #displayName Should be unique across the various patterns.
          - displayName: "Errors"
            pattern: "ERROR"
            matchExactString: true
            caseSensitive: true
            printMatchedString: true
```
The ```encoding``` field is not mandatory and can be disregarded in the config.yml unless you're working with non-UTF8 files. 

3. To get only the occurrences of a configured pattern and not the exact pattern match, simply set the ```printMatchedString``` field to false. 
By default, an Occurrences metric is initialized with 0 for each configured pattern, and can be used to create alerts and health rules. 

### 3. Metric Character Replacers

This section can be used to replace any characters in a match with the specified characters. They come into effect only a match is found for the 
original pattern. We have pre-configured three metric character replacers for characters considered invalid by the AppDynamics Metric Browser. 

```
#Replaces characters in metric name with the specified characters.
# "replace" takes any regular expression
# "replaceWith" takes the string to replace the matched characters
metricCharacterReplacer:
    - replace: ":"
      replaceWith: ";"
    - replace: "\\|"
      replaceWith: "#"
    - replace: "\\,"
      replaceWith: "#"
```

### 4. Number of Threads 
The extension uses one thread per configured log, and one thread per log file within. Let's consider our initial example: 

```logs:
     - displayName: "Test Log"
       logDirectory: "/Users/XYZ/MyApplication/logs"
       logName: "myLog.log*"
       searchStrings:
           #displayName Should be unique across the various patterns.
          - displayName: "Errors"
            pattern: "ERROR"
            matchExactString: true
            caseSensitive: true
            printMatchedString: true
```

Assuming that your logger settings make myLog.log rollover to a max of five files (myLog.log.1 to myLog.log.5), the number of threads needed in this case would be 7. 
(One for the log directory, and six for the files within). 


### 5. Configuring the monitor.xml

Configure the path to the config.yml by editing the ```<task-arguments>``` in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/LogMonitor/` directory: 

```
<task-arguments>
     <!-- config file-->
     <argument name="config-file" is-required="true" default-value="monitors/LogMonitor/config.yml" />
      ....
</task-arguments>
```

Restart the machine agent once this is done. 

## Metrics

The extension publishes the following metrics: 

1. File size in bytes 
2. Occurrences of each configured pattern 
3. Occurrences of matched strings, if ```printMatchedString``` is set to true 

## Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following [document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130) for how to use the Extensions WorkBench

## Troubleshooting
Please follow the steps listed in the [extensions troubleshooting document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might face during the installation of the extension. If these don't solve your issue, please follow the last step on the troubleshooting-document to contact the support team.

## Support Tickets
If after going through the Troubleshooting Document, you haven't been able to get your extension working, please file a ticket with the following information.

1. Stop the running machine agent .
2. Delete all existing logs under <MachineAgent>/logs .
3. Please enable debug logging by editing the file <MachineAgent>/conf/logging/log4j.xml. Change the level value of the following <logger> elements to debug. 
   ```
   <logger name="com.singularity">
   <logger name="com.appdynamics">
     ```
4. Start the machine agent and please let it run for 10 mins. Then zip and upload all the logs in the directory <MachineAgent>/logs/*.
5. Attach the zipped <MachineAgent>/conf/* directory.
6. Attach the zipped <MachineAgent>/monitors/LogMonitor directory.

For any support related questions, you can also contact help@appdynamics.com.

## Contributing
Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/log-monitoring-extension).

## Version
|          Name            |  Version   |
|--------------------------|------------|
|Extension Version         |3.0.0       |
|Controller Compatibility  |4.0 or Later|
|Last Update               |08/22/2018 |
|List of Changes           |[Change log](https://github.com/Appdynamics/log-monitoring-extension/blob/log-monitoring-extension-3.0.0/changelog.md) |

