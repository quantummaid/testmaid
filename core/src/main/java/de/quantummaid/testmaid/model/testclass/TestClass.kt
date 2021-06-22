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

package de.quantummaid.testmaid.model.testclass

import de.quantummaid.injectmaid.api.Injector
import de.quantummaid.testmaid.model.Timings
import de.quantummaid.testmaid.model.testcase.TestCaseData


interface TestClass {
    fun register(testClassData: TestClassData)

    fun skip(reason: String)

    fun prepare(testClassData: TestClassData, parentInjector: Injector)

    fun postpare()

    fun registerTestCase(testCaseData: TestCaseData)

    fun skipTestCase(testCaseData: TestCaseData, reason: String)

    fun prepareTestCase(testCaseData: TestCaseData)

    fun postpareTestCase(testCaseData: TestCaseData, error: Throwable?)

    fun timings(): Timings

    fun canProvideDependency(dependencyType: Class<Any>): Boolean
    fun canProvideTestCaseDependency(testCaseData: TestCaseData, dependencyType: Class<Any>): Boolean

    fun resolveDependency(dependencyType: Class<Any>): Any
    fun resolveTestCaseDependency(testCaseData: TestCaseData, dependencyType: Class<Any>): Any
}
