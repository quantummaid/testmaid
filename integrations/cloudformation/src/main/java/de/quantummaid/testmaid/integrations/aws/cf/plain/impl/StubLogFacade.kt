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

import de.quantummaid.testmaid.aws.cf.internal.RCALogFacade
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.*
import software.amazon.awssdk.services.cloudformation.model.Stack
import kotlin.time.Duration

class StubLogFacade(val logToStdOut: Boolean) : CloudFormationServiceLogFacade, RCALogFacade {
    override fun infoCreatingStack(stackDefinition: StackDefinition, timeout: Duration) {
        logToStdOut("Creating Stack ${stackDefinition.stackName}")
    }

    override fun infoCreatedStack(detailedStackInformation: DetailedStackInformation, timeout: Duration) {
        logToStdOut("Created Stack ${detailedStackInformation.stackName}")
    }

    override fun infoUpdatingStack(stackDefinition: StackDefinition, timeout: Duration) {
        logToStdOut("Updating Stack ${stackDefinition.stackName}")
    }

    override fun infoUpdatedStack(detailedStackInformation: DetailedStackInformation, timeout: Duration) {
        logToStdOut("Updated Stack ${detailedStackInformation.stackName}")
    }

    override fun infoDeletingStack(stackId: StackId, stackName: StackName, timeout: Duration) {
        logToStdOut("Deleting Stack ${stackName} with id ${stackId}")
    }

    override fun infoDeletedStack(stackId: StackId, stackName: StackName, timeout: Duration) {
        logToStdOut("Deleted Stack ${stackName} with id ${stackId}")
    }

    private fun logToStdOut(message: String) {
        if (logToStdOut) {
            println(message)
        }
    }

    override fun errorCreateStackFailed(
        stack: Stack,
        stackDefinition: StackDefinition,
        failedResources: List<SimpleResourceStatus.Failed>
    ) {
        logToStdOut("Failed to create stack ${stack.stackName()}, current state: ${stack.stackStatusAsString()}, failedResources: ${failedResources}")
    }

    override fun errorUpdateStackFailed(
        stack: Stack,
        stackDefinition: StackDefinition,
        failedResources: List<SimpleResourceStatus.Failed>
    ) {
        logToStdOut("Failed to update stack ${stack.stackName()}, current state: ${stack.stackStatusAsString()}, failedResources: ${failedResources}")
    }

    override fun errorDeleteStackFailed(stack: Stack, failedResources: List<SimpleResourceStatus.Failed>) {
        logToStdOut("Failed to delete stack ${stack.stackName()}, current state: ${stack.stackStatusAsString()}, failedResources: ${failedResources}")
    }
}
