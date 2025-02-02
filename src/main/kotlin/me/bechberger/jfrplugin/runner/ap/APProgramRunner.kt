package me.bechberger.jfrplugin.runner.ap

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.impl.DefaultJavaProgramRunner
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction
import com.intellij.execution.target.TargetEnvironmentAwareRunProfileState
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import me.bechberger.jfrplugin.config.deleteJFRFile
import me.bechberger.jfrplugin.config.jfrVirtualFile
import me.bechberger.jfrplugin.runner.jfr.JFRProgramRunner.Companion.loadFile
import me.bechberger.jfrplugin.util.isAsyncProfilerSupported
import org.jetbrains.concurrency.Promise

class APProgramRunner : DefaultJavaProgramRunner() {

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return isAsyncProfilerSupported() && try {
            (
                executorId == APExecutor.EXECUTOR_ID &&
                    profile !is RunConfigurationWithSuppressedDefaultRunAction &&
                    profile is RunConfigurationBase<*>
                )
        } catch (_: Exception) {
            false
        }
    }

    @Throws(ExecutionException::class)
    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        environment.project.deleteJFRFile()
        val descriptor = super.doExecute(state, environment)
        workWithDescriptor(descriptor, environment.project)
        return descriptor
    }

    override fun doExecuteAsync(
        state: TargetEnvironmentAwareRunProfileState,
        env: ExecutionEnvironment
    ): Promise<RunContentDescriptor?> {
        env.project.deleteJFRFile()
        return super.doExecuteAsync(state, env)
            .then { descriptor -> workWithDescriptor(descriptor, env.project); return@then descriptor }
    }

    fun workWithDescriptor(descriptor: RunContentDescriptor?, project: Project) {
        if (descriptor != null) {
            val processHandler = descriptor.processHandler
            processHandler?.addProcessListener(object : CapturingProcessAdapter() {
                override fun processTerminated(event: ProcessEvent) {
                    super.processTerminated(event)
                    loadFile(project)
                }
            })
        }
    }

    override fun getRunnerId() = RUNNER_ID

    companion object {
        const val RUNNER_ID = "Java Profile Runner"
    }
}
