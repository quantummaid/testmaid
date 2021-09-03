/*
 * Copyright (c) 2021 Richard Hauswald - https://quantummaid.de/.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.quantummaid.testmaid.integrations.aws.cf.plain.impl

import de.quantummaid.testmaid.aws.cf.exceptions.DeleteStackFailed
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.CloudFormationServiceLogFacade
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.StackId
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.StackName
import de.quantummaid.testmaid.util.PollingResult
import de.quantummaid.testmaid.util.TimeoutPoller
import software.amazon.awssdk.services.cloudformation.CloudFormationClient
import software.amazon.awssdk.services.cloudformation.model.StackStatus
import kotlin.time.Duration

internal class DeleteStackOperation(
    private val describeStackOperation: DescribeStackOperation,
    private val stackProgressOperation: StackProgressOperation,
    private val logFacade: CloudFormationServiceLogFacade,
    private val client: CloudFormationClient,
    private val timeoutPoller: TimeoutPoller = TimeoutPoller()
) {
    fun deleteByName(stackName: StackName, timeout: Duration) {
        val stack = describeStackOperation.describeSingleStack(stackName)
        if (stack != null) {
            val stackId = StackId(stack.stackId())
            deleteStack(stackId, stackName, timeout)
        }
    }

    fun deleteStack(stackId: StackId, stackName: StackName, timeout: Duration) {
        logFacade.infoDeletingStack(stackId, stackName, timeout)

        client.deleteStack { it.stackName(stackId.mappingValue()) }

        val withTimeoutResult = timeoutPoller.withTimeout(timeout, Duration.seconds(1)) {
            val stack = describeStackOperation.describeSingleStack(stackId)
            when {
                stack == null -> {
                    PollingResult.Done(stack)
                }
                stack.stackStatus() == StackStatus.DELETE_COMPLETE -> {
                    PollingResult.Done(stack)
                }
                else -> {
                    val stackProgress = stackProgressOperation.stackProgress(stack)
                    println("${stack.stackName()}: $stackProgress")
                    PollingResult.NotDoneYet(stack)
                }
            }
        }
        when (withTimeoutResult) {
            is PollingResult.Done -> {
                logFacade.infoDeletedStack(stackId, stackName, timeout)
            }
            is PollingResult.NotDoneYet -> {
                val sdkStack = withTimeoutResult.information
                throw DeleteStackFailed(sdkStack)
            }
        }
    }
}
