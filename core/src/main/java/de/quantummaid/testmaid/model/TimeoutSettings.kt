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

package de.quantummaid.testmaid.model

import de.quantummaid.testmaid.model.testcase.TestCaseData
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * There are 3 ways to register test parameters in InjectMaid that are costly(in terms of execution time) to create:
 * - As EAGER_SINGLETON
 *      PrepareTestCase -> injectMaid.enterScope -> createTestParameter
 *      ResolveParametersForTestCase -> injectMaid.lookupInstance -> creation already done
 *      executeTestCase
 * - As DEFAULT_SINGLETON with default singleton mode on demand
 *      PrepareTestCase -> injectMaid.enterScope -> nothing
 *      ResolveParametersForTestCase -> injectMaid.lookupInstance -> createTestParameter
 *      executeTestCase
 * - As PROTOTYPE
 *      PrepareTestCase -> injectMaid.enterScope -> nothing
 *      ResolveParametersForTestCase -> injectMaid.lookupInstance -> createTestParameter
 *      executeTestCase
 *
 * For testing it is assumed - until proven otherwise - that EAGER does not provide any benefit but does introduce
 * complexity for timeout specifications. Hence there are only timeout values regarding parameter creation and cleanup
 */
data class TimeoutSettings(
    val createTestParameterTimeout: Duration = seconds(10),
    val cleanupTestParametersTimeout: Duration = seconds(10)
)

class CreateTestParameterTimeout(dependant: String, dependencyType: Class<*>, cause: Throwable) : Exception(
    "Timeout creating test parameter of type ${dependencyType} for ${dependant}",
    cause
)

class CleanupTestParameterTimeout(dependant: String, cause: Throwable) : Exception(
    "Timeout cleaning up after ${dependant}",
    cause
)
