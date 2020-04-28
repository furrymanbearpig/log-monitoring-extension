package AE_LogMonitoringExtension.buildTypes

import AE_LogMonitoringExtension.publishCommitStatus
import AE_LogMonitoringExtension.vcsRoots.AE_LogMonitoringExtension
import AE_LogMonitoringExtension.withDefaults
import jetbrains.buildServer.configs.kotlin.v2018_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.schedule

object AE_LogMonitoringExtension_Build : BuildType({
    uuid = "f236de2f-f85c-43b6-904a-233dc6c28f43"
    name = "Log Monitoring Extension Build"

    withDefaults()

    steps {
        maven {
            goals = "clean install"
            mavenVersion = defaultProvidedVersion()
            jdkHome = "%env.JDK_18%"
            userSettingsSelection = "teamcity-settings"
        }
    }

    triggers {
        vcs {
        }
        schedule {
            schedulingPolicy = cron {
                hours = "4"
            }
            branchFilter = "+:master"
            triggerBuild = always()
            withPendingChangesOnly = false
            param("revisionRule", "lastFinished")
            param("dayOfWeek", "SUN-SAT")
        }
    }

    artifactRules = """
    +:target/LogMonitor-*.zip => target/
""".trimIndent()

    publishCommitStatus()
})