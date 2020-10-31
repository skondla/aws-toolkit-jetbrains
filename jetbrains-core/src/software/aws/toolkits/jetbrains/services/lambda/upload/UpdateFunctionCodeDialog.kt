// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.lambda.upload

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import org.jetbrains.annotations.TestOnly
import software.aws.toolkits.jetbrains.core.help.HelpIds
import software.aws.toolkits.jetbrains.services.lambda.Lambda.findPsiElementsForHandler
import software.aws.toolkits.jetbrains.services.lambda.LambdaBuilder
import software.aws.toolkits.jetbrains.services.lambda.LambdaFunction
import software.aws.toolkits.jetbrains.services.lambda.runtimeGroup
import software.aws.toolkits.jetbrains.services.lambda.sam.SamOptions
import software.aws.toolkits.jetbrains.settings.UpdateLambdaSettings
import software.aws.toolkits.jetbrains.utils.execution.steps.StepExecutor
import software.aws.toolkits.resources.message
import software.aws.toolkits.telemetry.LambdaTelemetry
import software.aws.toolkits.telemetry.Result
import java.nio.file.Paths
import javax.swing.JComponent

class UpdateFunctionCodeDialog(private val project: Project, private val initialSettings: LambdaFunction) : DialogWrapper(project) {
    private val view = UpdateFunctionCodePanel(project)
    private val updateSettings = UpdateLambdaSettings.getInstance(initialSettings.arn)

    init {
        super.init()
        title = message("lambda.upload.updateCode.title", initialSettings.name)
        setOKButtonText(message("general.update_button"))

        view.handlerPanel.handler.text = initialSettings.handler
        view.handlerPanel.setRuntime(initialSettings.runtime)

        loadSettings()
    }

    override fun createCenterPanel(): JComponent? = view.content

    override fun getPreferredFocusedComponent(): JComponent? = view.handlerPanel.handler

    override fun doValidate(): ValidationInfo? = view.validatePanel()

    override fun doCancelAction() {
        LambdaTelemetry.editFunction(project, result = Result.Cancelled)
        super.doCancelAction()
    }

    override fun doOKAction() {
        saveSettings()
        upsertLambdaCode()
    }

    override fun getHelpId(): String? = HelpIds.UPDATE_FUNCTION_CODE_DIALOG.id

    private fun upsertLambdaCode() {
        if (!okAction.isEnabled) {
            return
        }
        FileDocumentManager.getInstance().saveAllDocuments()

        val functionDetails = FunctionUploadDetails(
            name = initialSettings.name,
            handler = view.handlerPanel.handler.text,
            iamRole = initialSettings.role,
            runtime = initialSettings.runtime,
            description = initialSettings.description,
            envVars = initialSettings.envVariables ?: emptyMap(),
            timeout = initialSettings.timeout,
            memorySize = initialSettings.memorySize,
            xrayEnabled = initialSettings.xrayEnabled,
            samOptions = SamOptions( // TODO: Remove this from details data class
                buildInContainer = view.buildSettings.buildInContainerCheckbox.isSelected
            )
        )

        // TODO: Move this so we can share it with CreateFunctionDialog, but don't move it lower since passing PsiELement lower needs to go away since
        // it is causing customer complaints. We need to prompt for baseDir and try to infer it if we can but only as a default value...
        val element = findPsiElementsForHandler(project, functionDetails.runtime, functionDetails.handler).first()
        val psiFile = element.containingFile
        val module = ModuleUtil.findModuleForFile(psiFile)
            ?: throw IllegalStateException("Failed to locate module for $psiFile")
        val lambdaBuilder = initialSettings.runtime.runtimeGroup?.let { LambdaBuilder.getInstanceOrNull(it) }
            ?: throw IllegalStateException("LambdaBuilder for ${initialSettings.runtime} not found")

        val s3Bucket = view.codeStorage.sourceBucket.selected() as String
        val workflow = updateLambdaCodeWorkflow(
            project,
            functionDetails, // TODO: Really we only need runtime, handler, codeUri so we should make a dataclass for just that
            lambdaBuilder.getBuildDirectory(module), // TODO ... how do we kill module here? Can we use a temp dir?
            Paths.get(lambdaBuilder.handlerBaseDirectory(module, element)), // TODO: Make this return a Path
            s3Bucket,
            functionDetails.samOptions,
            functionDetails.takeIf { it.handler != initialSettings.handler }
        )

        // TODO: Add callback tp startExecution, onSuccess, onError? onFinished(e: Exception?)?
        StepExecutor(project, "Update Function Code", workflow, initialSettings.name).startExecution()
//         { _, error ->
//            when (error) {
//                null -> {
//                    notifyInfo(
//                        project = project,
//                        title = message("lambda.service_name"),
//                        content = message("lambda.function.code_updated.notification", functionDetails.name)
//                    )
//                    LambdaTelemetry.editFunction(project, update = false, result = Result.Succeeded)
//                }
//                is Exception -> {
//                    error.notifyError(project = project, title = message("lambda.service_name"))
//                    LambdaTelemetry.editFunction(project, update = false, result = Result.Failed)
//                }
//            }
//        }
        close(OK_EXIT_CODE)
    }

    private fun loadSettings() {
        view.codeStorage.sourceBucket.selectedItem = updateSettings.bucketName
        view.buildSettings.buildInContainerCheckbox.isSelected = updateSettings.useContainer ?: false
    }

    private fun saveSettings() {
        updateSettings.bucketName = view.codeStorage.sourceBucket.selectedItem?.toString()
        updateSettings.useContainer = view.buildSettings.buildInContainerCheckbox.isSelected
    }

    @TestOnly
    fun getViewForTestAssertions() = view
}
