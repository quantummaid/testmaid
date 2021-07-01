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

package de.quantummaid.testmaid.internal

import de.quantummaid.injectmaid.InjectMaid
import de.quantummaid.testmaid.TestMaid.Companion.buildTestMaid
import de.quantummaid.testmaid.internal.testclass.TestCaseAlreadyRegisteredException
import de.quantummaid.testmaid.internal.testclass.TestCaseNotRegisteredException
import de.quantummaid.testmaid.model.testcase.TestCaseData
import de.quantummaid.testmaid.model.testclass.TestClassData
import de.quantummaid.testmaid.model.testclass.TestClassScope
import de.quantummaid.testmaid.model.testsuite.TestSuiteScope
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.InputStream

class IntegrationApiSpecs {

    @Test
    fun testRegisterTestCaseOnClassThatDoesNotExist() {
        buildTestMaid(InjectMaid.anInjectMaid()).use { testMaid ->
            val testClassData = TestClassData("foo", String::class.java, emptySet())
            val testCaseData = TestCaseData("foo", String::class.java.methods[0], emptySet(), testClassData)
            var exception: NullPointerException? = null
            try {
                testMaid.integrationApi.registerTestCase(testCaseData)
            } catch (e: NullPointerException) {
                exception = e
            }
            assertNotNull(exception)
        }
    }

    @Test
    fun testRegisterTestCaseTwice() {
        buildTestMaid(InjectMaid.anInjectMaid()).use { testMaid ->
            val testClassData = TestClassData("foo", String::class.java, emptySet())
            testMaid.integrationApi.registerTestClass(testClassData)
            testMaid.integrationApi.testClassStart(testClassData)
            val testCaseData = TestCaseData("foo", String::class.java.methods[0], emptySet(), testClassData)
            testMaid.integrationApi.registerTestCase(testCaseData)
            var exception: TestCaseAlreadyRegisteredException? = null
            try {
                testMaid.integrationApi.registerTestCase(testCaseData)
            } catch (e: TestCaseAlreadyRegisteredException) {
                exception = e
            }
            assertNotNull(exception)
        }
    }

    @Test
    fun testPrepareAndPostpareTestCaseThatIsNotRegistered() {
        buildTestMaid(InjectMaid.anInjectMaid()).use { testMaid ->
            val testClassData = TestClassData("foo", String::class.java, emptySet())
            testMaid.integrationApi.registerTestClass(testClassData)
            testMaid.integrationApi.testClassStart(testClassData)
            val testCaseData = TestCaseData("foo", String::class.java.methods[0], emptySet(), testClassData)
            var exception: TestCaseNotRegisteredException? = null
            try {
                testMaid.integrationApi.testCaseStart(testCaseData)
            } catch (e: TestCaseNotRegisteredException) {
                exception = e
            }
            assertNotNull(exception)
            exception = null
            try {
                testMaid.integrationApi.testCaseFinish(testCaseData, null)
            } catch (e: TestCaseNotRegisteredException) {
                exception = e
            }
            assertNotNull(exception)
        }
    }

    @Test
    fun testPostpareTestCaseWithFailure() {
        buildTestMaid(InjectMaid.anInjectMaid()).use { testMaid ->
            val testClassData = TestClassData("foo", String::class.java, emptySet())
            testMaid.integrationApi.registerTestClass(testClassData)
            testMaid.integrationApi.testClassStart(testClassData)
            val testCaseData = TestCaseData("foo", String::class.java.methods[0], emptySet(), testClassData)
            testMaid.integrationApi.registerTestCase(testCaseData)
            testMaid.integrationApi.testCaseStart(testCaseData)
            testMaid.integrationApi.testCaseFinish(testCaseData, UnsupportedOperationException())
        }
    }

    @Test
    fun testCanProvideTestClassDependency() {
        val injectMaidBuilder = InjectMaid.anInjectMaid()
        injectMaidBuilder.withScope(TestSuiteScope::class.java) { testSuiteInjectMaid ->
            testSuiteInjectMaid.withScope(TestClassScope::class.java) { testClassInjectMaid ->
                testClassInjectMaid.withCustomType(String::class.java) { "foo" }
            }
        }
        buildTestMaid(injectMaidBuilder).use { testMaid ->
            val testClassData = TestClassData("foo", String::class.java, emptySet())

            testMaid.integrationApi.registerTestClass(testClassData)
            testMaid.integrationApi.testClassStart(testClassData)
            val canProvideString =
                testMaid.injectionApi.canProvideTestClassDependency(testClassData, String::class.java)
            Assertions.assertTrue(canProvideString)

            val dependency = testMaid.injectionApi.resolveTestClassDependency(testClassData, String::class.java)
            Assertions.assertEquals("foo", dependency)

            val canProvideInputStream =
                testMaid.injectionApi.canProvideTestClassDependency(testClassData, InputStream::class.java)
            Assertions.assertFalse(canProvideInputStream)
        }
    }

    @Test
    fun testCanProvideTestCaseDependency() {
        val injectMaidBuilder = InjectMaid.anInjectMaid()
        injectMaidBuilder.withScope(TestSuiteScope::class.java) { testSuiteInjectMaid ->
            testSuiteInjectMaid.withScope(TestClassScope::class.java) { testClassInjectMaid ->
                testClassInjectMaid.withCustomType(String::class.java) { "foo" }
            }
        }
        buildTestMaid(injectMaidBuilder).use { testMaid ->
            val testClassData = TestClassData("foo", String::class.java, emptySet())

            testMaid.integrationApi.registerTestClass(testClassData)
            testMaid.integrationApi.testClassStart(testClassData)

            val testCaseData = TestCaseData("foo", String::class.java.methods[0], emptySet(), testClassData)
            var exception: TestCaseNotRegisteredException? = null
            try {
                testMaid.injectionApi.canProvideTestCaseDependency(testCaseData, String::class.java)
            } catch (e: TestCaseNotRegisteredException) {
                exception = e
            }
            assertNotNull(exception)

            exception = null
            try {
                testMaid.injectionApi.resolveTestCaseDependency(testCaseData, String::class.java)
            } catch (e: TestCaseNotRegisteredException) {
                exception = e
            }
            assertNotNull(exception)
        }
    }
}