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

package de.quantummaid.testmaid.model.testsuite

import de.quantummaid.testmaid.ExecutionDecision
import de.quantummaid.testmaid.model.testcase.TestCaseData
import de.quantummaid.testmaid.model.testclass.TestClassData

interface TestSuite : AutoCloseable {
    fun postpareTestSuite()

    fun registerTestClass(testClassData: TestClassData): ExecutionDecision
    fun prepareTestClass(testClassData: TestClassData)
    fun postpareTestClass(testClassData: TestClassData)

    fun registerTestCase(testCaseData: TestCaseData): ExecutionDecision
    fun prepareTestCase(testCaseData: TestCaseData)
    fun postpareTestCase(testCaseData: TestCaseData, error: Throwable?)

    fun canProvideTestClassDependency(testClassData: TestClassData, dependencyType: Class<*>): Boolean
    fun canProvideTestCaseDependency(testCaseData: TestCaseData, dependencyType: Class<*>): Boolean
    fun <T> resolveTestClassDependency(testClassData: TestClassData, dependencyType: Class<T>): T
    fun <T> resolveTestCaseDependency(testCaseData: TestCaseData, dependencyType: Class<T>): T
}
