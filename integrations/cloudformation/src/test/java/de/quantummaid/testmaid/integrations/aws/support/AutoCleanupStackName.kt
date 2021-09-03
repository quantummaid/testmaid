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

package de.quantummaid.testmaid.integrations.aws.support

import de.quantummaid.testmaid.integrations.aws.cf.plain.api.StackName
import de.quantummaid.testmaid.integrations.aws.cf.plain.impl.SimpleResourceStatus.Companion.notDeletedSdkStates
import de.quantummaid.testmaid.util.PollingResult
import de.quantummaid.testmaid.util.TimeoutPoller
import software.amazon.awssdk.services.cloudformation.CloudFormationClient
import software.amazon.awssdk.services.cloudformation.model.StackStatus
import software.amazon.awssdk.services.cloudformation.model.StackSummary
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AutoCleanupStackName(
    val stackName: StackName, private val cloudFormationClient: CloudFormationClient
) : AutoCloseable {

    init {

        val stacks: Iterable<StackSummary> = cloudFormationClient
            .listStacksPaginator { it.stackStatusFilters(notDeletedSdkStates()) }
            .stackSummaries()
        println("Found stacks ${stacks.map { it.stackName() }}")
        val existingStack = stacks
            .filter {
                it.stackName() == stackName.mappingValue()
            }
            .singleOrNull()
        println("Found stack ${existingStack}")
        if (existingStack != null) {
            println("deleting existing stack $stackName")
            cloudFormationClient.deleteStack { it.stackName(stackName.mappingValue()) }
            TimeoutPoller().withTimeoutOrFail(minutes(10), seconds(1)) {
                val stack = cloudFormationClient
                    .listStacksPaginator { }
                    .stackSummaries().filter {
                        it.stackName() == stackName.mappingValue()
                    }
                    .singleOrNull()
                if (stack != null) {
                    if (stack.stackStatus() == StackStatus.DELETE_COMPLETE) {
                        PollingResult.Done(stack)
                    } else {
                        PollingResult.NotDoneYet(stack)
                    }
                } else {
                    PollingResult.NotDoneYet(StackSummary.builder().build())
                }
            }
        }
    }

    override fun close() {
        val stackSummaries = cloudFormationClient
            .listStacksPaginator { }
            .stackSummaries()
        val matchingStacksWithSameName = stackSummaries
            .filter { it.stackStatus() != StackStatus.DELETE_COMPLETE }
            .filter { it.stackName() == stackName.mappingValue() }
        val stackToDelete = if (matchingStacksWithSameName.size > 1) {
            val stackInformation = matchingStacksWithSameName.map {
                "StackId: ${it.stackId()}, StackName: ${it.stackName()}, Status: ${it.stackStatusAsString()}, StatusReason: ${it.stackStatusReason()}"
            }
            throw UnsupportedOperationException("Found multiple stacks named ${stackName.mappingValue()}: $stackInformation")
        } else {
            matchingStacksWithSameName.firstOrNull()
        }
        if (stackToDelete != null) {
            cloudFormationClient.deleteStack { it.stackName(stackToDelete.stackId()) }
            TimeoutPoller().withTimeoutOrFail(minutes(10), seconds(1)) {
                val stack = cloudFormationClient
                    .listStacksPaginator { }
                    .stackSummaries()
                    .first { it.stackId() == stackToDelete.stackId() }

                if (stack.stackStatus() == StackStatus.DELETE_COMPLETE) {
                    PollingResult.Done(stack)
                } else {
                    PollingResult.NotDoneYet(stack)
                }
            }
        }
    }
}
