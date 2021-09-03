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

import de.quantummaid.testmaid.integrations.aws.cf.plain.api.*
import de.quantummaid.testmaid.util.close
import kotlinx.coroutines.*
import kotlin.time.Duration

internal class CloudFormationServiceImpl(
        private val createStackOperation: CreateStackOperation,
        private val listStacksWithPrefixOperation: ListStacksWithPrefixOperation,
        private val updateStackOperation: UpdateStackOperation,
        private val deleteStackOperation: DeleteStackOperation
) : CloudFormationService, AutoCloseable {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + CoroutineName("CloudFormationServiceImpl"))

    override fun createStack(stackDefinition: StackDefinition, timeout: Duration): DetailedStackInformation {
        return createStackOperation.createStack(stackDefinition, timeout)
    }

    override fun updateStack(stackDefinition: StackDefinition, timeout: Duration): DetailedStackInformation {
        return updateStackOperation.updateStack(stackDefinition, timeout)
    }

    override fun listStacksWithPrefix(stackPrefix: StackPrefix): List<StackReference> {
        return listStacksWithPrefixOperation.listWithPrefix(stackPrefix)
    }

    override fun deleteStack(stackName: StackName, timeout: Duration) {
        deleteStackOperation.deleteByName(stackName, timeout)
    }

    override fun deleteStack(stackId: StackId, stackName: StackName, timeout: Duration) {
        deleteStackOperation.deleteStack(stackId, stackName, timeout)
    }

    override fun deleteStackAsync(stackId: StackId, stackName: StackName, timeout: Duration): CompletableDeferred<Unit> {
        val retVal = CompletableDeferred<Unit>()

        coroutineScope.launch {
            try {
                deleteStack(stackId, stackName, timeout)
                retVal.complete(Unit)
            } catch (e: Exception) {
                retVal.completeExceptionally(e)
            }
        }
        return retVal
    }

    override fun close() {
        this.coroutineScope.close()
    }

}
