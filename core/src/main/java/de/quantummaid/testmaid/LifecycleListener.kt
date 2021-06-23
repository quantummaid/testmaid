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

package de.quantummaid.testmaid

import de.quantummaid.injectmaid.api.Injector
import de.quantummaid.testmaid.model.testcase.TestCaseData
import de.quantummaid.testmaid.model.testclass.TestClassData


interface LifecycleListener {
    fun startup(globalScopedInjector: Injector) {}

    fun beforeTestSuite(testSuiteScopedInjector: Injector) {}
    fun beforeTestClass(testClassData: TestClassData, testClassScopedInjector: Injector) {}
    fun beforeTestCase(testCaseData: TestCaseData, testCaseScopedInjector: Injector) {}

    fun afterTestCase(testCaseResult: TestCaseResult, testCaseScopedInjector: Injector) {}
    fun afterTestClass(testClassResult: TestClassResult, testClassScopedInjector: Injector) {}
    fun afterTestSuite(testSuiteResult: TestSuiteResult, testSuiteScopedInjector: Injector) {}

    fun shutdown(globalScopedInjector: Injector) {}
}
