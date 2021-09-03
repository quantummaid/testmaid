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
package de.quantummaid.testmaid.tests.example

import de.quantummaid.testmaid.model.CleanupTestParameterTimeout
import de.quantummaid.testmaid.model.CreateTestParameterTimeout
import de.quantummaid.testmaid.tests.example.fixtures.*
import de.quantummaid.testmaid.tests.example.testsupport.DomainTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterResolutionException
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler
import org.junit.jupiter.api.fail
import kotlin.time.Duration.Companion.seconds

/**
 * TimeoutSettings are set within the DomainTest.kt TestMaidBuilder call
 */
@DomainTest
@ExtendWith(VerifyTimeoutExceptions::class)
class TimeoutSpecs {

    @Test
    fun longRunningTestCase() {
        Thread.sleep(seconds(3).inWholeMilliseconds)
    }

    @Test
    fun longRunningTestFixtureCreationTestCase(longInitTimeFixture: LongInitTimeFixture) {
        assertNotNull(longInitTimeFixture)
    }

    @Test
    fun longRunningTestFixtureCleanupTestCase(longCleanupTimeFixture: LongCleanupTimeFixture) {
        assertNotNull(longCleanupTimeFixture)
    }

    @Test
    fun longRunningTestFixtureInitAndCleanupTestCase(longInitAndCleanupTimeFixture: LongInitAndCleanupTimeFixture) {
        assertNotNull(longInitAndCleanupTimeFixture)
    }

    @Test
    fun tooLongRunningTestFixtureCreationTestCase(tooLongInitTimeFixture: TooLongInitTimeFixture) {
        fail("This test case should not execute since it's parameter resolution should fail.")
    }

    @Test
    @Disabled("Blocked by https://github.com/junit-team/junit5/issues/2666")
    fun tooLongRunningTestFixtureCleanupTestCase(tooLongCleanupTimeFixture: TooLongCleanupTimeFixture) {
        assertNotNull(tooLongCleanupTimeFixture)
    }

    @Test
    @Disabled("Blocked by https://github.com/junit-team/junit5/issues/2666")
    fun tooLongRunningTestFixtureInitAndCleanupTestCase(tooLongInitAndCleanupTimeFixture: TooLongInitAndCleanupTimeFixture) {
        fail("This test case should not execute since it's parameter resolution should fail.")
    }
}

class VerifyTimeoutExceptions : TestExecutionExceptionHandler {
    private val expectedToFailMethodNames = listOf(
        "tooLongRunningTestFixtureCreationTestCase",
        "tooLongRunningTestFixtureCleanupTestCase",
        "tooLongRunningTestFixtureInitAndCleanupTestCase",
    )


    override fun handleTestExecutionException(context: ExtensionContext, throwable: Throwable) {
        var expected = false
        context.testMethod.ifPresent {
            if (expectedToFailMethodNames.contains(it.name)) {
                val exceptionToInspect = if (throwable is ParameterResolutionException) {
                    throwable.cause
                } else {
                    throwable
                }
                expected = exceptionToInspect is CreateTestParameterTimeout
                        || exceptionToInspect is CleanupTestParameterTimeout
            }
        }
        if (!expected)
            throw throwable
    }
}
