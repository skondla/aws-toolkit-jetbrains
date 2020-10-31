// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.lambda.upload.steps

import com.intellij.execution.configurations.GeneralCommandLine
import software.aws.toolkits.jetbrains.services.lambda.sam.SamOptions
import software.aws.toolkits.jetbrains.services.lambda.sam.samBuildCommand
import software.aws.toolkits.jetbrains.utils.execution.steps.Context
import java.nio.file.Path

class BuildLambda(private val templatePath: Path, private val buildDir: Path, private val samOptions: SamOptions) : SamCliStep() {
    override val stepName: String = "SAM Build"

    override fun constructCommandLine(context: Context): GeneralCommandLine = getCli().samBuildCommand(
        templatePath = templatePath,
        buildDir = buildDir,
        environmentVariables = emptyMap(),
        samOptions = samOptions
    )
}
