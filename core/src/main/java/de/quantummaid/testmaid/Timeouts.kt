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

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Timeouts(
    val defaultTimeout: Duration,
    val prepareTestCaseTimeout: Duration,
    val postpareTestCaseTimeout: Duration,
    val resolveTestCaseDependencyTimeout: Duration,
    val prepareTestClassTimeout: Duration,
    val postpareTestClassTimeout: Duration,
    val resolveTestClassDependencyTimeout: Duration,
    val prepareTestSuiteTimeout: Duration,
    val postpareTestSuiteTimeout: Duration
) {
    companion object {
        fun simplifiedTimeouts(
            defaultTimeout: Duration = seconds(10),
            dependencyConstructionTimeout: Duration = defaultTimeout,
            dependencyDestructionTimeout: Duration = defaultTimeout,
            testClassSetupTimeout: Duration = defaultTimeout,
            testClassTeardownTimeout: Duration = defaultTimeout,
            testSuiteSetupTimeout: Duration = defaultTimeout,
            testSuiteTeardownTimeout: Duration = defaultTimeout,
        ): Timeouts {
            return Timeouts(
                defaultTimeout = defaultTimeout,
                prepareTestCaseTimeout = dependencyConstructionTimeout,
                postpareTestCaseTimeout = dependencyDestructionTimeout,
                resolveTestCaseDependencyTimeout = dependencyConstructionTimeout,
                prepareTestClassTimeout = testClassSetupTimeout,
                postpareTestClassTimeout = testClassTeardownTimeout,
                resolveTestClassDependencyTimeout = dependencyConstructionTimeout,
                prepareTestSuiteTimeout = testSuiteSetupTimeout,
                postpareTestSuiteTimeout = testSuiteTeardownTimeout,
            )
        }

        fun Duration.delegated(depth: Int): Duration {
            return this.plus(seconds(1).times(depth))
        }
    }
}