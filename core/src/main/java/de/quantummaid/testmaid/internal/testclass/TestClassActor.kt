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

package de.quantummaid.testmaid.internal.testclass

import de.quantummaid.injectmaid.api.Injector
import de.quantummaid.testmaid.internal.statemachine.StateMachineActor
import de.quantummaid.testmaid.model.testcase.TestCaseData
import de.quantummaid.testmaid.model.testclass.TestClass
import de.quantummaid.testmaid.model.testclass.TestClassData
import kotlinx.coroutines.runBlocking

internal class TestClassActor private constructor(internal val delegate: StateMachineActor<TestClassState, TestClassMessage>) :
    TestClass {
    companion object {
        fun aTestClassActor(): TestClassActor {
            return TestClassActor(testClassStateMachine())
        }
    }

    override fun register(testClassData: TestClassData) {
        delegate.signalAwaitingSuccess(RegisterTestClass(testClassData))
    }

    override fun skip(reason: String) {
        delegate.signalAwaitingSuccess(SkipTestClass(reason))
    }

    override fun prepare(testClassData: TestClassData, parentInjector: Injector) {
        val msg = PrepareTestClass(testClassData, parentInjector)
        delegate.signalAwaitingSuccess(msg)
    }

    override fun postpare() {
        delegate.signalAwaitingSuccess(PostpareTestClass)
    }

    override fun registerTestCase(testCaseData: TestCaseData): AutoCloseable {
        val msg = RegisterTestCase(testCaseData)
        delegate.signalAwaitingSuccess(msg)
        return runBlocking { msg.actor.await() }
    }

    override fun skipTestCase(testCaseData: TestCaseData, reason: String) {
        delegate.signalAwaitingSuccess(SkipTestCase(testCaseData, reason))
    }

    override fun prepareTestCase(testCaseData: TestCaseData) {
        delegate.signalAwaitingSuccess(PrepareTestCase(testCaseData))
    }

    override fun postpareTestCase(testCaseData: TestCaseData, error: Throwable?) {
        delegate.signalAwaitingSuccess(PostpareTestCase(testCaseData, error))
    }

    override fun canProvideDependency(dependencyType: Class<*>): Boolean {
        val msg = CanProvideParameter(dependencyType)
        delegate.signalAwaitingSuccess(msg)
        return runBlocking { msg.result.await() }
    }

    override fun canProvideTestCaseDependency(testCaseData: TestCaseData, dependencyType: Class<*>): Boolean {
        val msg = CanProvideTestCaseParameter(testCaseData, dependencyType)
        delegate.signalAwaitingSuccess(msg)
        return runBlocking { msg.result.await() }
    }

    override fun resolveDependency(dependencyType: Class<*>): Any {
        val msg = ResolveParameter(dependencyType)
        delegate.signalAwaitingSuccess(msg)
        return runBlocking { msg.result.await() }
    }

    override fun resolveTestCaseDependency(testCaseData: TestCaseData, dependencyType: Class<*>): Any {
        val msg = ResolveTestCaseParameter(testCaseData, dependencyType)
        delegate.signalAwaitingSuccess(msg)
        return runBlocking { msg.result.await() }
    }
}
