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

import de.quantummaid.testmaid.aws.cf.exceptions.CreateStackFailed
import de.quantummaid.testmaid.aws.cf.exceptions.DeleteStackFailed
import de.quantummaid.testmaid.aws.cf.exceptions.StackNotFound
import de.quantummaid.testmaid.aws.cf.exceptions.UpdateStackFailed
import de.quantummaid.testmaid.aws.cf.internal.Mapper
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.*
import de.quantummaid.testmaid.util.PollingResult
import de.quantummaid.testmaid.util.TimeoutPoller
import software.amazon.awssdk.services.cloudformation.CloudFormationClient
import software.amazon.awssdk.services.cloudformation.model.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class CloudFormationServiceImpl(
    private val client: CloudFormationClient,
    private val logFacade: CloudFormationServiceLogFacade,
    private val timeoutPoller: TimeoutPoller = TimeoutPoller()
) : CloudFormationService {

    override fun createStack(stackDefinition: StackDefinition, timeout: Duration): CreatedStack {
        logFacade.infoCreatingStack(stackDefinition, timeout)

        val createStackResponse = client.createStack(Mapper.mapToCreateStackRequest(stackDefinition, timeout))
        val stackId = StackId(createStackResponse.stackId())

        val withTimeoutResult = timeoutPoller.withTimeout(timeout, seconds(1)) {
            val stack = describeSingleStack(stackId)
            when {
                stack == null -> {
                    PollingResult.NotDoneYet(stack)
                }
                stack.stackStatus() == StackStatus.CREATE_COMPLETE -> {
                    PollingResult.Done(stack)
                }
                Mapper.CREATE_FAILURE_STATES.contains(stack.stackStatus()) -> {
                    throw CreateStackFailed(stackDefinition, stack)
                }
                else -> {
                    val response = client.describeStackResources { it.stackName(stack.stackId()) }
                    val stackResources = response.stackResources()
                    val todo = stackResources.map {
                        SimpleResourceStatus.fromSdkResource(it)
                    }.filterNot { it is SimpleResourceStatus.Success }
                        .map { "${it.intel.logicalId}[${it.intel.status}]" }
                    println("${stackDefinition.stackName}: ${todo}")
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
                throw CreateStackFailed(stackDefinition, sdkStack)
            }
        }
    }

    override fun updateStack(stackDefinition: StackDefinition, timeout: Duration): CreatedStack {
        logFacade.infoUpdatingStack(stackDefinition, timeout)

        val createStackResponse = client.updateStack(Mapper.mapToUpdateStackRequest(stackDefinition))
        val stackId = StackId(createStackResponse.stackId())

        val withTimeoutResult = timeoutPoller.withTimeout(timeout, seconds(1)) {
            val stack = describeSingleStack(stackId)
            when {
                stack == null -> {
                    PollingResult.NotDoneYet(stack)
                }
                stack.stackStatus() == StackStatus.UPDATE_COMPLETE -> {
                    PollingResult.Done(stack)
                }
                Mapper.UPDATE_FAILURE_STATES.contains(stack.stackStatus()) -> {
                    throw CreateStackFailed(stackDefinition, stack)
                }
                else -> {
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
                throw UpdateStackFailed(stackDefinition, sdkStack)
            }
        }
    }

    override fun deleteStack(stackName: StackName, timeout: Duration) {
        val stack = describeSingleStack(stackName)
        if (stack != null) {
            println(stack.stackId())
            val stackId = StackId(stack.stackId())
            deleteStack(stackId, stackName, timeout)
        } else {
            throw StackNotFound(stackName)
        }
    }

    override fun deleteStack(stackId: StackId, stackName: StackName, timeout: Duration) {
        logFacade.infoDeletingStack(stackId, stackName, timeout)
        val deleteStackRequest = DeleteStackRequest.builder()
            .stackName(stackId.mappingValue())
            .build()
        client.deleteStack(deleteStackRequest)

        val withTimeoutResult = timeoutPoller.withTimeout(timeout, seconds(1)) {
            val stack = describeSingleStack(stackId)
            when {
                stack == null -> {
                    PollingResult.Done(stack)
                }
                stack.stackStatus() == StackStatus.DELETE_COMPLETE -> {
                    PollingResult.Done(stack)
                }
                else -> {
                    val response = client.describeStackResources { it.stackName(stack.stackId()) }
                    val stackResources = response.stackResources()
                    val todo = stackResources.map {
                        SimpleResourceStatus.fromSdkResource(it)
                    }.filterNot { it is SimpleResourceStatus.Success }
                        .map { "${it.intel.logicalId}[${it.intel.status}]" }
                    println("${stack.stackName()}: ${todo}")
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

    fun describeSingleStack(stackId: StackId): Stack? {
        val request = DescribeStacksRequest.builder()
            .stackName(stackId.mappingValue())
            .build()
        return describeSingleStack(request)
    }

    fun describeSingleStack(stackName: StackName): Stack? {
        val request = DescribeStacksRequest.builder()
            .stackName(stackName.mappingValue())
            .build()
        return describeSingleStack(request)
    }

    fun describeSingleStack(request: DescribeStacksRequest): Stack? {
        val describeStacksResponse = try {
            client.describeStacks(request)
        } catch (e: CloudFormationException) {
            if (Mapper.isStackNotFoundException(e)) {
                return null
            } else {
                throw e
            }
        }
        return if (describeStacksResponse.stacks().size > 1) {
            val stacksAsString = describeStacksResponse.stacks()
                .joinToString(", ") {
                    "${it.stackName()}[${it.stackStatusAsString()}]"
                }
            throw UnsupportedOperationException(
                "Describing single stack with name/id '${request.stackName()}' resulted in " +
                        "more than one stack: ${stacksAsString}"
            )
        } else {
            describeStacksResponse.stacks().single()
        }
    }
}
