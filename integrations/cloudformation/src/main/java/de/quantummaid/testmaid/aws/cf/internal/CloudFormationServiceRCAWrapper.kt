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

package de.quantummaid.testmaid.aws.cf.internal

import de.quantummaid.testmaid.aws.cf.exceptions.CreateStackFailed
import de.quantummaid.testmaid.aws.cf.exceptions.DeleteStackFailed
import de.quantummaid.testmaid.aws.cf.exceptions.UpdateStackFailed
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.*
import de.quantummaid.testmaid.integrations.aws.cf.plain.impl.SimpleResourceStatus
import kotlinx.coroutines.CompletableDeferred
import software.amazon.awssdk.services.cloudformation.CloudFormationClient
import software.amazon.awssdk.services.cloudformation.model.Stack
import kotlin.time.Duration


internal class CloudFormationServiceRCAWrapper(
    private val delegate: CloudFormationService,
    private val client: CloudFormationClient,
    private val logFacade: RCALogFacade
) : CloudFormationService {
    override fun listStacksWithPrefix(stackPrefix: StackPrefix): List<StackReference> {
        return delegate.listStacksWithPrefix(stackPrefix)
    }

    override fun createStack(stackDefinition: StackDefinition, timeout: Duration): DetailedStackInformation {
        try {
            return delegate.createStack(stackDefinition, timeout)
        } catch (e: CreateStackFailed) {
            val stack = e.stack
            val failedResources = failedResourcesOf(stack)
            logFacade.errorCreateStackFailed(e.stack, e.stackDefinition, failedResources)
            throw e
        }
    }

    override fun updateStack(stackDefinition: StackDefinition, timeout: Duration): DetailedStackInformation {
        try {
            return delegate.updateStack(stackDefinition, timeout)
        } catch (e: UpdateStackFailed) {
            val stack = e.stack
            val failedResources = failedResourcesOf(stack)
            logFacade.errorUpdateStackFailed(e.stack, e.stackDefinition, failedResources)
            throw e
        }
    }

    override fun deleteStack(stackName: StackName, timeout: Duration) {
        try {
            delegate.deleteStack(stackName, timeout)
        } catch (e: DeleteStackFailed) {
            val stack = e.stack
            val failedResources = failedResourcesOf(stack)
            logFacade.errorDeleteStackFailed(e.stack, failedResources)
            throw e
        }
    }

    override fun deleteStack(stackId: StackId, stackName: StackName, timeout: Duration) {
        try {
            delegate.deleteStack(stackId, stackName, timeout)
        } catch (e: DeleteStackFailed) {
            val stack = e.stack
            val failedResources = failedResourcesOf(stack)
            logFacade.errorDeleteStackFailed(e.stack, failedResources)
            throw e
        }
    }

    override fun deleteStackAsync(stackId: StackId, stackName: StackName, timeout: Duration): CompletableDeferred<Unit> {
        return delegate.deleteStackAsync(stackId, stackName, timeout)
    }

    private fun failedResourcesOf(stack: Stack): List<SimpleResourceStatus.Failed> {
        val stackId = stack.stackId()
        val response = client.describeStackResources { it.stackName(stackId) }
        val stackResources = response.stackResources()
        val failedResources = stackResources.map {
            SimpleResourceStatus.fromSdkResource(it)
        }.filterIsInstance<SimpleResourceStatus.Failed>()
        return failedResources
    }

}
