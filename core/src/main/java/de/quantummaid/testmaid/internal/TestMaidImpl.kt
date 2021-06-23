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

import de.quantummaid.testmaid.ExecutionDecision
import de.quantummaid.testmaid.TestMaid
import de.quantummaid.testmaid.TestMaidInjectionApi
import de.quantummaid.testmaid.TestMaidIntegrationApi
import de.quantummaid.testmaid.model.testcase.TestCaseData
import de.quantummaid.testmaid.model.testclass.TestClassData
import de.quantummaid.testmaid.model.testsuite.TestSuite

internal class TestMaidImpl(private val testSuite: TestSuite) : TestMaid, TestMaidIntegrationApi, TestMaidInjectionApi {

    override val integrationApi: TestMaidIntegrationApi
        get() = this
    override val injectionApi: TestMaidInjectionApi
        get() = this

    override fun registerTestClass(testClassData: TestClassData): ExecutionDecision {
        return testSuite.registerTestClass(testClassData)
    }

    override fun registerTestCase(testCaseData: TestCaseData): ExecutionDecision {
        return testSuite.registerTestCase(testCaseData)
    }

    override fun testClassStart(testClassData: TestClassData) {
        testSuite.prepareTestClass(testClassData)
    }

    override fun testClassFinish(testClassData: TestClassData) {
        testSuite.postpareTestClass(testClassData)
    }

    override fun testCaseStart(testCaseData: TestCaseData) {
        testSuite.prepareTestCase(testCaseData)
    }

    override fun testCaseFinish(testCaseData: TestCaseData, error: Throwable?) {
        testSuite.postpareTestCase(testCaseData, error)
    }

    override fun canProvideTestClassDependency(testClassData: TestClassData, dependencyType: Class<Any>): Boolean {
        return testSuite.canProvideTestClassDependency(testClassData, dependencyType)
    }

    override fun canProvideTestCaseDependency(testCaseData: TestCaseData, dependencyType: Class<Any>): Boolean {
        return testSuite.canProvideTestCaseDependency(testCaseData, dependencyType)
    }

    override fun <T> resolveTestClassDependency(testClassData: TestClassData, dependencyType: Class<T>): T {
        return testSuite.resolveTestClassDependency(testClassData, dependencyType)
    }

    override fun <T> resolveTestCaseDependency(testCaseData: TestCaseData, dependencyType: Class<T>): T {
        return testSuite.resolveTestCaseDependency(testCaseData, dependencyType)
    }
}
