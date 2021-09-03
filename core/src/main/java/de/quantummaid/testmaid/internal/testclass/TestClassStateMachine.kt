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
import de.quantummaid.testmaid.internal.statemachine.StateMachineBuilder
import de.quantummaid.testmaid.internal.statemachine.StateMachineBuilder.Companion.aStateMachineUsing
import de.quantummaid.testmaid.internal.testcase.TestCaseActor
import de.quantummaid.testmaid.model.TimeoutSettings
import de.quantummaid.testmaid.model.testcase.TestCase
import de.quantummaid.testmaid.model.testcase.TestCaseData
import de.quantummaid.testmaid.model.testclass.TestClassData
import de.quantummaid.testmaid.model.testclass.TestClassScope
import kotlinx.coroutines.CompletableDeferred

internal fun testClassStateMachine(timeoutSettings: TimeoutSettings): StateMachineActor<TestClassState, TestClassMessage> {
    return testClassStateMachineBuilder(timeoutSettings).build()
}

private fun testClassStateMachineBuilder(
    timeoutSettings: TimeoutSettings
): StateMachineBuilder<TestClassState, TestClassMessage> {
    return aStateMachineUsing<TestClassState, TestClassMessage>("TestClassActor")
        .withInitialState(InitialTestClass)
        .withEndStateSuperClass<TestClassEndState>()
        .withTransition<InitialTestClass, RegisterTestClass, RegisteredTestClass> {
            RegisteredTestClass(it.testClassData)
        }
        .withTransition<RegisteredTestClass, SkipTestClass, SkippedTestClass> {
            SkippedTestClass(testClassData, it.reason)
        }
        .withTransition<RegisteredTestClass, PrepareTestClass, ReadyToExecuteTestClass> {
            val injector = it.parentInjector.enterScope(TestClassScope(testClassData))
            ReadyToExecuteTestClass(testClassData, injector, mutableMapOf())
        }
        .withTransition<ReadyToExecuteTestClass, RegisterTestCase, ReadyToExecuteTestClass> {
            if (!testCases.contains(it.testCaseData)) {
                val testCase = TestCaseActor.aTestCaseActor(timeoutSettings)
                testCase.register(it.testCaseData)
                testCases[it.testCaseData] = testCase
                it.actor.complete(testCase.delegate)
            } else {
                throw TestCaseAlreadyRegisteredException(it.testCaseData, testCases)
            }
            this
        }
        .withTransition<ReadyToExecuteTestClass, PrepareTestCase, ReadyToExecuteTestClass> {
            val testCase = testCases[it.testCaseData] ?: throw TestCaseNotRegisteredException(
                it.testCaseData, this.testCases
            )
            testCase.prepare(injector)
            this
        }
        .withTransition<ReadyToExecuteTestClass, PostpareTestCase, ReadyToExecuteTestClass> {
            val testCase = testCases[it.testCaseData] ?: throw TestCaseNotRegisteredException(
                it.testCaseData, this.testCases
            )
            if (it.error == null) {
                testCase.pass()
            } else {
                testCase.fail(it.error)
            }
            testCase.postpare()
            this
        }
        .withTransition<ReadyToExecuteTestClass, PostpareTestClass, FinishedTestClass> {
            this.injector.close()
            FinishedTestClass(testClassData, testCases)
        }
        .withQuery<ReadyToExecuteTestClass, CanProvideParameter> {
            val canInstantiate = this.injector.canInstantiate(it.dependencyType)
            it.result.complete(canInstantiate)
        }
        .withQuery<ReadyToExecuteTestClass, CanProvideTestCaseParameter> {
            val testCase = testCases[it.testCaseData] ?: throw TestCaseNotRegisteredException(
                it.testCaseData, this.testCases
            )
            val canInstantiate = testCase.canProvideDependency(it.dependencyType)
            it.result.complete(canInstantiate)
        }
        .withQuery<ReadyToExecuteTestClass, ResolveParameter> {
            val resolution = this.injector.getInstance(it.dependencyType)
            it.result.complete(resolution)
        }
        .withQuery<ReadyToExecuteTestClass, ResolveTestCaseParameter> {
            val testCase = testCases[it.testCaseData] ?: throw TestCaseNotRegisteredException(
                it.testCaseData, this.testCases
            )
            val resolution = testCase.resolveDependency(it.dependencyType)
            it.result.complete(resolution)
        }
        .withTransition<ReadyToExecuteTestClass, SkipTestCase, ReadyToExecuteTestClass> {
            val testCase = testCases[it.testCaseData] ?: throw TestCaseNotRegisteredException(
                it.testCaseData, this.testCases
            )
            testCase.skip(it.reason)
            this
        }
}

internal interface TestClassState

internal interface RegisteredTestClassState : TestClassState {
    val testClassData: TestClassData
}

internal interface TestClassEndState : RegisteredTestClassState

internal object InitialTestClass : TestClassState

internal data class RegisteredTestClass(
    override val testClassData: TestClassData
) :
    RegisteredTestClassState

internal data class ReadyToExecuteTestClass(
    override val testClassData: TestClassData,
    val injector: Injector,
    val testCases: MutableMap<TestCaseData, TestCase>
) : RegisteredTestClassState

internal data class FinishedTestClass(
    override val testClassData: TestClassData,
    val testCases: MutableMap<TestCaseData, TestCase>
) :
    RegisteredTestClassState

internal data class SkippedTestClass(
    override val testClassData: TestClassData,
    val reason: String
) : TestClassEndState

internal interface TestClassMessage
internal data class RegisterTestClass(val testClassData: TestClassData) : TestClassMessage
internal data class SkipTestClass(val reason: String) : TestClassMessage
internal data class PrepareTestClass(val testClassData: TestClassData, val parentInjector: Injector) :
    TestClassMessage

internal object PostpareTestClass : TestClassMessage

internal data class RegisterTestCase(
    val testCaseData: TestCaseData,
    val actor: CompletableDeferred<AutoCloseable> = CompletableDeferred()
) : TestClassMessage

internal data class SkipTestCase(val testCaseData: TestCaseData, val reason: String) : TestClassMessage
internal data class PrepareTestCase(val testCaseData: TestCaseData) : TestClassMessage
internal data class PostpareTestCase(val testCaseData: TestCaseData, val error: Throwable?) : TestClassMessage

internal data class CanProvideParameter(
    val dependencyType: Class<*>,
    val result: CompletableDeferred<Boolean> = CompletableDeferred()
) : TestClassMessage

internal data class CanProvideTestCaseParameter(
    val testCaseData: TestCaseData,
    val dependencyType: Class<*>,
    val result: CompletableDeferred<Boolean> = CompletableDeferred()
) : TestClassMessage

internal data class ResolveParameter(
    val dependencyType: Class<*>,
    val result: CompletableDeferred<Any> = CompletableDeferred()
) : TestClassMessage

internal data class ResolveTestCaseParameter(
    val testCaseData: TestCaseData,
    val dependencyType: Class<*>,
    val result: CompletableDeferred<Any> = CompletableDeferred()
) : TestClassMessage
