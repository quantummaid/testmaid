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

package de.quantummaid.testmaid.integrations.aws.cf.plain.api

import de.quantummaid.testmaid.integrations.aws.cf.plain.TemplateFixtures
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.Parameters.Companion.parameters
import de.quantummaid.testmaid.util.AutoCleanedCoroutineScope
import de.quantummaid.testmaid.integrations.aws.support.AutoCleanupStackName
import de.quantummaid.testmaid.integrations.aws.support.AwsEndToEndTest
import de.quantummaid.testmaid.util.AutoCleanupStackPrefix
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException
import kotlin.time.Duration.Companion.minutes

@AwsEndToEndTest
internal class CloudFormationServiceTest(
        private val sut: CloudFormationService
) {
    @Test
    fun testSimpleStack(autoCleanupStackName: AutoCleanupStackName) {
        val detailedStackInformation = sut.createStack(fastAndSimpleTestStack(autoCleanupStackName.stackName))
        println(detailedStackInformation.outputs)
    }

    @Test
    fun testListByPrefix(autoCleanupStackPrefix: AutoCleanupStackPrefix, scope: AutoCleanedCoroutineScope) {
        val uniqueStackNames = (1..3).map { autoCleanupStackPrefix.generateUniqueName() }
        val errors = mutableListOf<Exception>()

        val stacks = uniqueStackNames
                .map {
                    val deferred = CompletableDeferred<DetailedStackInformation>()
                    scope.launch {
                        try {
                            val detailedStackInformation = sut.createStack(fastAndSimpleTestStack(it))
                            deferred.complete(detailedStackInformation)
                        } catch (e: Exception) {
                            deferred.completeExceptionally(e)
                        }
                    }
                    deferred
                }.mapNotNull {
                    runBlocking {
                        try {
                            it.await()
                        } catch (e: Exception) {
                            errors.add(e)
                            null
                        }
                    }
                }
        if (errors.isNotEmpty()) {
            val exception = UnsupportedOperationException("Errors creating stacks")
            errors.forEach { exception.addSuppressed(it) }
            throw exception
        }
    }

    @Test
    fun testCreateFailsForMissingParameters() {
        val exception = assertThrows<CloudFormationException> {
            sut.createStack(
                    StackDefinition(
                            StackName("HelloWorld"),
                            Body(TemplateFixtures.EIP_V1)
                    )
            )
        }
        assertTrue(exception.message!!.startsWith("Parameters: [StackIdentifier] must have values"))
    }

    private fun fastAndSimpleTestStack(name: StackName): StackDefinition {
        val body = Body(TemplateFixtures.EIP_V1)
        val parameters = parameters("StackIdentifier" to name.mappingValue())
        return StackDefinition(name, body, parameters)
    }
}
