// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.utils.execution.steps

import com.intellij.util.ExceptionUtil

class ConsoleMessageEmitter(private val stepName: String) : MessageEmitter {
    override fun createChild(stepName: String, hidden: Boolean): MessageEmitter {
        return ConsoleMessageEmitter(stepName)
    }

    override fun startStep() {
        println("[$stepName] [StartEvent]")
    }

    override fun finishSuccessfully() {
        println("[$stepName] [FinishEvent] Success")
    }

    override fun finishExceptionally(e: Throwable) {
        println("[$stepName] [FinishedExceptionally] ${ExceptionUtil.getNonEmptyMessage(e, e.javaClass.simpleName)}")
    }

    override fun emitMessage(message: String, isError: Boolean) {
        println("[$stepName] $message")
    }
}
