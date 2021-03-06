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

package de.quantummaid.testmaid.internal.testsuite

import de.quantummaid.injectmaid.api.Injector
import de.quantummaid.testmaid.ExecutionDecision
import de.quantummaid.testmaid.SkipDecider
import de.quantummaid.testmaid.internal.statemachine.StateMachineActor
import de.quantummaid.testmaid.model.testcase.TestCaseData
import de.quantummaid.testmaid.model.testclass.TestClassData
import de.quantummaid.testmaid.model.testsuite.TestSuite
import kotlinx.coroutines.runBlocking

class TestSuiteActor private constructor(private val delegate: StateMachineActor<TestSuiteState, TestSuiteMessage>) :
    TestSuite {
    private val actorsToClose = mutableListOf<AutoCloseable>()
        .apply { add(delegate) }

    companion object {
        fun aTestSuiteActor(
            injector: Injector,
            skipDecider: SkipDecider,
        ): TestSuiteActor {
            val delegate = testSuiteStateMachine()
            delegate.signalAwaitingSuccess(PrepareTestSuite(injector, skipDecider))
            return TestSuiteActor(delegate)
        }
    }

    override fun postpareTestSuite() {
        delegate.signalAwaitingSuccess(AllTestsFinished)
    }

    override fun registerTestClass(testClassData: TestClassData): ExecutionDecision {
        val msg = RegisterTestClass(testClassData)
        delegate.signalAwaitingSuccess(msg)

        val actor = runBlocking { msg.actor.await() }
        actorsToClose.add(actor)

        return runBlocking { msg.executionDecision.await() }
    }

    override fun prepareTestClass(testClassData: TestClassData) {
        delegate.signalAwaitingSuccess(PrepareTestClass(testClassData))
    }

    override fun postpareTestClass(testClassData: TestClassData) {
        delegate.signalAwaitingSuccess(PostpareTestClass(testClassData))
    }

    override fun registerTestCase(testCaseData: TestCaseData): ExecutionDecision {
        val msg = RegisterTestCase(testCaseData)
        delegate.signalAwaitingSuccess(msg)
        val actor = runBlocking { msg.actor.await() }
        actorsToClose.add(actor)
        return runBlocking { msg.executionDecision.await() }
    }

    override fun prepareTestCase(testCaseData: TestCaseData) {
        delegate.signalAwaitingSuccess(PrepareTestCase(testCaseData))
    }

    override fun postpareTestCase(testCaseData: TestCaseData, error: Throwable?) {
        delegate.signalAwaitingSuccess(PostpareTestCase(testCaseData, error))
    }

    override fun canProvideTestClassDependency(testClassData: TestClassData, dependencyType: Class<*>): Boolean {
        val msg = CanProvideTestClassParameter(testClassData, dependencyType)
        delegate.signalAwaitingSuccess(msg)
        return runBlocking { msg.result.await() }
    }

    override fun canProvideTestCaseDependency(testCaseData: TestCaseData, dependencyType: Class<*>): Boolean {
        val msg = CanProvideTestCaseParameter(testCaseData, dependencyType)
        delegate.signalAwaitingSuccess(msg)
        return runBlocking { msg.result.await() }
    }

    override fun <T> resolveTestClassDependency(testClassData: TestClassData, dependencyType: Class<T>): T {
        val msg = ResolveTestClassParameter(testClassData, dependencyType as Class<Any>)
        delegate.signalAwaitingSuccess(msg)
        return runBlocking { msg.result.await() } as T
    }

    override fun <T> resolveTestCaseDependency(testCaseData: TestCaseData, dependencyType: Class<T>): T {
        val msg = ResolveTestCaseParameter(testCaseData, dependencyType as Class<Any>)
        delegate.signalAwaitingSuccess(msg)
        return runBlocking { msg.result.await() } as T
    }

    override fun close() {
        actorsToClose.forEach { it.close() }
    }
}
