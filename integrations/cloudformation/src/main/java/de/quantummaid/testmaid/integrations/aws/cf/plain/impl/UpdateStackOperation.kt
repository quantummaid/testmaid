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

import de.quantummaid.testmaid.aws.cf.exceptions.UpdateStackFailed
import de.quantummaid.testmaid.aws.cf.internal.Mapper
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.CloudFormationServiceLogFacade
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.DetailedStackInformation
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.StackDefinition
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.StackId
import de.quantummaid.testmaid.util.PollingResult
import de.quantummaid.testmaid.util.TimeoutPoller
import software.amazon.awssdk.services.cloudformation.CloudFormationClient
import software.amazon.awssdk.services.cloudformation.model.StackStatus
import kotlin.time.Duration

internal class UpdateStackOperation(
        private val describeStackOperation: DescribeStackOperation,
        private val stackProgressOperation: StackProgressOperation,
        private val describeFailedStackResourcesOperation: DescribeFailedStackResourcesOperation,
        private val logFacade: CloudFormationServiceLogFacade,
        private val client: CloudFormationClient,
        private val timeoutPoller: TimeoutPoller = TimeoutPoller()
) {
    fun updateStack(stackDefinition: StackDefinition, timeout: Duration): DetailedStackInformation {
        logFacade.infoUpdatingStack(stackDefinition, timeout)

        val createStackResponse = client.updateStack(Mapper.mapToUpdateStackRequest(stackDefinition))
        val stackId = StackId(createStackResponse.stackId())

        val withTimeoutResult = timeoutPoller.withTimeout(timeout, Duration.seconds(1)) {
            val stack = describeStackOperation.describeSingleStack(stackId)
            when {
                stack == null -> {
                    PollingResult.NotDoneYet(stack)
                }
                stack.stackStatus() == StackStatus.UPDATE_COMPLETE -> {
                    PollingResult.Done(stack)
                }
                Mapper.UPDATE_FAILURE_STATES.contains(stack.stackStatus()) -> {

                    val failedResources = describeFailedStackResourcesOperation.describeFailedResources(stackId)
                    throw UpdateStackFailed(stackDefinition, stack, failedResources)
                }
                else -> {
                    val stackProgress = stackProgressOperation.stackProgress(stack)
                    println("${stack.stackName()}: $stackProgress")
                    PollingResult.NotDoneYet(stack)
                }
            }
        }
        return when (withTimeoutResult) {
            is PollingResult.Done -> {
                val createdStack = Mapper.mapToCreatedStack(withTimeoutResult.result)
                logFacade.infoCreatedStack(createdStack, timeout)
                createdStack
            }
            is PollingResult.NotDoneYet -> {
                val sdkStack = withTimeoutResult.information!!
                val failedResources = describeFailedStackResourcesOperation.describeFailedResources(stackId)
                throw UpdateStackFailed(stackDefinition, sdkStack, failedResources)
            }
        }
    }
}
