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

package de.quantummaid.testmaid.util

import de.quantummaid.testmaid.integrations.aws.cf.plain.api.CloudFormationService
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.StackName
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.StackNameBuilder
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.StackPrefix
import de.quantummaid.testmaid.model.testcase.TestCaseData
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration

class AutoCleanupStackPrefix(
        stackNameBuilder: StackNameBuilder,
        private val testCaseData: TestCaseData,
        private val cloudFormationService: CloudFormationService,
        private val deleteTimeout: Duration = CloudFormationService.defaultTimeout
) : AutoCloseable {
    val prefix = stackNameBuilder.prefixForTestCase(testCaseData.name)

    init {
        deleteStacksWithMyPrefix()
    }

    override fun close() {
        deleteStacksWithMyPrefix()
    }

    fun generateUniqueName(): StackName {
        return prefix.generateUniqueName()
    }

    private fun deleteStacksWithMyPrefix() {
        val withPrefix = cloudFormationService.listStacksWithPrefix(StackPrefix("testmaid-aws-Richard"))
        val failedStacks = withPrefix.map {
            it to cloudFormationService.deleteStackAsync(it.stackId, it.stackName, deleteTimeout)
        }.mapNotNull {
            runBlocking {
                try {
                    it.second.await()
                    null
                } catch (e: Exception) {
                    Exception("Could not delete stack ${it.first}", e)
                }
            }
        }
        if (failedStacks.isNotEmpty()) {
            val exception = Exception("Could not delete all stacks")
            failedStacks.forEach { exception.addSuppressed(it) }
            throw exception
        }
    }
}
