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

import de.quantummaid.testmaid.aws.cf.exceptions.StackNotFound
import de.quantummaid.testmaid.integrations.aws.TagKey
import de.quantummaid.testmaid.integrations.aws.TagValue
import de.quantummaid.testmaid.integrations.aws.Tags
import de.quantummaid.testmaid.integrations.aws.cf.plain.TemplateFixtures
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.*
import de.quantummaid.testmaid.integrations.aws.cf.plain.impl.CloudFormationServiceImpl
import de.quantummaid.testmaid.integrations.aws.cf.plain.impl.StubLogFacade
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.cloudformation.CloudFormationClient

internal class CloudFormationServiceImplTest {

    @Test
    fun testCreateUpdateDelete() {
        val client = CloudFormationClient.create()
        val stubLogFacade = StubLogFacade(true)
        val serviceImpl = CloudFormationServiceImpl(client, stubLogFacade)
        val sut = CloudFormationServiceRCAWrapper(serviceImpl, client, stubLogFacade)
        val stackName = StackName("CloudFormationServiceImplTest-updateStack")
        try {
            sut.deleteStack(stackName)
        } catch (e: StackNotFound) {
            //Do nothing, we can continue
        }

        val createdStack = sut.createStack(
            StackDefinition(
                stackName,
                Body(TemplateFixtures.EIP_V1),
                Parameters(
                    listOf(
                        Pair(
                            ParameterKey("StackIdentifier"),
                            ParameterValue(stackName.mappingValue())
                        )
                    )
                ),
                Tags(
                    listOf(
                        Pair(TagKey("Owner"), TagValue("CloudFormationServiceImplTest.updateStack"))
                    )
                )
            )
        )
        assertTrue("[0-9.]{7,15}".toRegex().matches(createdStack[OutputName("SampleEIP")].mappingValue()))

        val updatedStack = sut.updateStack(
            StackDefinition(
                stackName,
                Body(TemplateFixtures.EIP_V2),
                Parameters(
                    listOf(
                        Pair(
                            ParameterKey("StackIdentifier"),
                            ParameterValue(stackName.mappingValue())
                        )
                    )
                ),
                Tags(
                    listOf(
                        Pair(TagKey("Owner"), TagValue("CloudFormationServiceImplTest.updateStack"))
                    )
                )
            )
        )
        assertTrue("[0-9.]{7,15}".toRegex().matches(updatedStack[OutputName("ExampleEIP")].mappingValue()))

        sut.deleteStack(stackName)
    }
}
