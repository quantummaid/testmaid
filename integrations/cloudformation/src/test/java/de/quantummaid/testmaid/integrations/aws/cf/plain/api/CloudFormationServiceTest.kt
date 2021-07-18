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
import de.quantummaid.testmaid.integrations.aws.cf.plain.impl.StubLogFacade
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.services.cloudformation.CloudFormationClient
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException

internal class CloudFormationServiceTest {
    private val cloudFormationClient = CloudFormationClient.builder().build()
    private val sut = CloudFormationService.cloudFormationService(cloudFormationClient, StubLogFacade(true))
    private val stackNameBuilder =
        StackNameBuilder(StackPrefix("testmaid-aws"), StackPrefix(System.getenv("RuntimeId")!!))

    @Test
    fun testCreateStackWithoutOptionalParameters() {
        val createdStack = sut.createStack(
            StackDefinition(
                stackNameBuilder.forTestCase("testCreateStackWithoutOptionalParameters"),
                Body(TemplateFixtures.EIP_V1),
                parameters("StackIdentifier" to "HelloStackId")
            )
        )

        println(createdStack)
        assertEquals(
            "testmaid-aws-Richard-testCreateStackWithoutOptionalParameters",
            createdStack.stackName.mappingValue()
        )
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
}
