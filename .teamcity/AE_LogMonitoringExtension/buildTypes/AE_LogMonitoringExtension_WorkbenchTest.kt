package AE_LogMonitoringExtension.buildTypes

import AE_LogMonitoringExtension.publishCommitStatus
import AE_LogMonitoringExtension.vcsRoots.AE_LogMonitoringExtension
import AE_LogMonitoringExtension.withDefaults
import jetbrains.buildServer.configs.kotlin.v2018_2.BuildStep
import jetbrains.buildServer.configs.kotlin.v2018_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2018_2.FailureAction
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.exec
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.vcs

object AE_LogMonitoringExtension_WorkbenchTest : BuildType({
    uuid = "ff722de5-bbc9-4423-b46b-b86c91944485"
    name = "Test Workbench mode"

    withDefaults()

    steps {
        exec {
            path = "make"
            arguments = "workbenchTest"
        }
        exec {
            executionMode = BuildStep.ExecutionMode.ALWAYS
            path = "make"
            arguments = "dockerClean"
        }
    }

    triggers {
        vcs {
        }
    }

    dependencies {
        dependency(AE_LogMonitoringExtension_Build) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }
            artifacts {
                artifactRules = """
                +:target/LogMonitor-*.zip => target/
            """.trimIndent()
            }
        }
    }

    publishCommitStatus()
})