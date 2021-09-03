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

package de.quantummaid.testmaid.internal.testcase

import de.quantummaid.injectmaid.api.Injector
import de.quantummaid.testmaid.internal.statemachine.StateMachineActor
import de.quantummaid.testmaid.model.CleanupTestParameterTimeout
import de.quantummaid.testmaid.model.CreateTestParameterTimeout
import de.quantummaid.testmaid.model.TimeoutSettings
import de.quantummaid.testmaid.model.testcase.TestCase
import de.quantummaid.testmaid.model.testcase.TestCaseData
import kotlinx.coroutines.runBlocking

internal class TestCaseActor private constructor(
    internal val delegate: StateMachineActor<TestCaseState, TestCaseMessage>,
    private val timeoutSettings: TimeoutSettings
) :
    TestCase {
    companion object {
        fun aTestCaseActor(timeoutSettings: TimeoutSettings): TestCaseActor {
            return TestCaseActor(testCaseStateMachine(), timeoutSettings)
        }
    }

    override fun register(testCaseData: TestCaseData) {
        val msg = RegisterTestCase(testCaseData)
        delegate.signalAwaitingSuccess(msg)
    }

    override fun skip(reason: String) {
        delegate.signalAwaitingSuccess(SkipTestCase(reason))
    }

    override fun prepare(parentInjector: Injector) {
        delegate.signalAwaitingSuccess(PrepareTestCase(parentInjector))
    }

    override fun pass() {
        delegate.signalAwaitingSuccess(TestCasePassed)
    }

    override fun fail(cause: Throwable) {
        delegate.signalAwaitingSuccess(TestCaseFailed(cause))
    }

    override fun postpare() {
        delegate.signalAwaitingSuccess(PostpareTestCase, timeoutSettings.cleanupTestParametersTimeout) {
            CleanupTestParameterTimeout(delegate.name, it)
        }
    }

    override fun canProvideDependency(dependencyType: Class<*>): Boolean {
        val msg = CanProvideParameter(dependencyType)
        delegate.signalAwaitingSuccess(msg)
        return runBlocking { msg.result.await() }
    }

    override fun resolveDependency(dependencyType: Class<*>): Any {
        val msg = ResolveParameter(dependencyType)
        delegate.signalAwaitingSuccess(msg, timeoutSettings.createTestParameterTimeout) {
            CreateTestParameterTimeout(delegate.name, dependencyType, it)
        }
        return runBlocking { msg.result.await() }
    }
}
