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

package de.quantummaid.testmaid.cloudformationenv

import de.quantummaid.testmaid.integrations.aws.cf.env.CloudFormationEnvironmentServiceCaching
import de.quantummaid.testmaid.localfs.LocalDirectory.Companion.targetDirectoryOfClass
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.StackDefinition
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.StackName
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.Body
import de.quantummaid.testmaid.integrations.aws.s3.BucketName
import de.quantummaid.testmaid.integrations.aws.s3.ObjectKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.*

internal class CloudFormationEnvironmentServiceCachingTest {
    companion object {
        private val cacheBaseDirectory = targetDirectoryOfClass(CloudFormationEnvironmentServiceCachingTest::class.java)
    }

    @Test
    fun smokeTest() {
        val environmentServiceStub = object : CloudFormationEnvironmentService {
            var createOrUpdateInvocationCount = 0

            override fun createOrUpdate(environmentDefinition: EnvironmentDefinition): Environment {
                createOrUpdateInvocationCount++
                return Environment(environmentDefinition)
            }

            override fun delete(environmentDefinition: EnvironmentDefinition) {
                fail("This should not be called by this test")
            }
        }
        val sut = CloudFormationEnvironmentServiceCaching(cacheBaseDirectory, environmentServiceStub)
        val codeFile = cacheBaseDirectory.createFile(UUID.randomUUID().toString())
        val environmentDefinition = EnvironmentDefinition(
            EnvironmentName("testCreateCacheIfNotExists${UUID.randomUUID()}"),
            StackDefinition(
                StackName("Kevin"),
                Body("")
            ),
            LambdaCode(
                codeFile,
                BucketName("non.existent"),
                ObjectKey("testCreateCacheIfNotExists/")
            )
        )
        val firstInvocationEnvironmentDefinition = sut.createOrUpdate(environmentDefinition)
        val secondInvocationEnvironmentDefinition = sut.createOrUpdate(environmentDefinition)
        val thirdInvocationEnvironmentDefinition = sut.createOrUpdate(environmentDefinition)

        assertEquals(1, environmentServiceStub.createOrUpdateInvocationCount)
        assertEquals(firstInvocationEnvironmentDefinition, secondInvocationEnvironmentDefinition)
        assertEquals(firstInvocationEnvironmentDefinition, thirdInvocationEnvironmentDefinition)
    }
}
