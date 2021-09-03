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

import de.quantummaid.injectmaid.InjectMaidBuilder
import de.quantummaid.testmaid.internal.TestMaidImpl
import de.quantummaid.testmaid.internal.testcase.testCaseStateMachine
import de.quantummaid.testmaid.internal.testclass.testClassStateMachine
import de.quantummaid.testmaid.internal.testsuite.TestSuiteActor
import de.quantummaid.testmaid.internal.testsuite.testSuiteStateMachine
import de.quantummaid.testmaid.model.TimeoutSettings
import de.quantummaid.testmaid.model.testcase.TestCaseScope
import de.quantummaid.testmaid.model.testclass.TestClassScope
import de.quantummaid.testmaid.model.testsuite.TestSuiteScope

interface TestMaid : AutoCloseable {
    val integrationApi: TestMaidIntegrationApi
    val injectionApi: TestMaidInjectionApi

    companion object {
        fun buildTestMaid(
            injectMaidBuilder: InjectMaidBuilder,
            skipDecider: SkipDecider = SkipDecider.alwaysExecute(),
            timeoutSettings: TimeoutSettings = TimeoutSettings()
        ): TestMaid {
            val injectMaid = injectMaidBuilder
                .withLifecycleManagement()
                .closeOnJvmShutdown()
                .withScope(TestSuiteScope::class.java) { testSuiteInjectMaid ->
                    testSuiteInjectMaid.withScope(TestClassScope::class.java) { testClassInjectMaid ->
                        testClassInjectMaid.withScope(TestCaseScope::class.java) { testCaseInjectMaid ->
                        }
                    }
                }
                .build()

            return TestMaidImpl(TestSuiteActor.aTestSuiteActor(injectMaid, skipDecider, timeoutSettings), injectMaid)
        }

        fun renderStateMachine(): String {
            val testSuiteStateMachine = "testsuite" to testSuiteStateMachine(TimeoutSettings()).renderStateMachine()
            val testClassStateMachine = "testclass" to testClassStateMachine(TimeoutSettings()).renderStateMachine()
            val testCaseStateMachine = "testcase" to testCaseStateMachine().renderStateMachine()
            return listOf(testSuiteStateMachine, testClassStateMachine, testCaseStateMachine)
                .flatMap { (name, statements) -> renderInSubgraph(name, statements) }
                .joinToString(prefix = "digraph G {\n", postfix = "\n}", separator = "\n")
        }
    }
}

private fun renderInSubgraph(name: String, statements: List<String>): List<String> {
    return listOf("subgraph cluster_$name {", "label = \"$name\";", "color=\"blue\";") + statements + "}"
}
