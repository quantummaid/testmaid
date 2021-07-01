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
import de.quantummaid.testmaid.internal.statemachine.StateMachineBuilder.Companion.aStateMachineUsing
import de.quantummaid.testmaid.model.testcase.TestCaseData
import de.quantummaid.testmaid.model.testcase.TestCaseScope
import kotlinx.coroutines.CompletableDeferred

internal fun testCaseStateMachine(): StateMachineActor<TestCaseState, TestCaseMessage> {
    return testCaseStateMachineBuilder.build()
}

private val testCaseStateMachineBuilder = aStateMachineUsing<TestCaseState, TestCaseMessage>()
    .withInitialState(InitialTestCase)
    .withEndStateSuperClass<TestCaseEndState>()
    .withTransition<InitialTestCase, RegisterTestCase, RegisteredTestCase> {
        RegisteredTestCase(it.testCaseData)
    }
    .withTransition<RegisteredTestCase, SkipTestCase, SkippedTestCase> {
        SkippedTestCase(testCaseData, it.reason)
    }
    .withTransition<RegisteredTestCase, PrepareTestCase, ReadyToExecuteTestCase> {
        val injector = it.parentInjector.enterScope(TestCaseScope(testCaseData))
        ReadyToExecuteTestCase(testCaseData, injector)
    }
    .withQuery<ReadyToExecuteTestCase, CanProvideParameter> {
        val canInstantiate = injector.canInstantiate(it.dependencyType)
        it.result.complete(canInstantiate)
    }
    .withQuery<ReadyToExecuteTestCase, ResolveParameter> {
        val resolution = injector.getInstance(it.dependencyType)
        it.result.complete(resolution)
    }
    .withTransition<ReadyToExecuteTestCase, TestCaseFailed, FailedTestCase> {
        FailedTestCase(testCaseData, injector, it.cause)
    }
    .withTransition<ReadyToExecuteTestCase, TestCasePassed, PassedTestCase> {
        PassedTestCase(testCaseData, injector)
    }
    .withTransition<PassedTestCase, PostpareTestCase, PostparedPassedTestCase> {
        injector.close()
        PostparedPassedTestCase(testCaseData)
    }
    .withTransition<FailedTestCase, PostpareTestCase, PostparedFailedTestCase> {
        injector.close()
        PostparedFailedTestCase
    }

internal interface TestCaseState

internal interface RegisteredTestCaseState : TestCaseState {
    val testCaseData: TestCaseData
}

internal interface TestCaseEndState : RegisteredTestCaseState

internal object InitialTestCase : TestCaseState

internal data class RegisteredTestCase(override val testCaseData: TestCaseData) :
    RegisteredTestCaseState

internal data class ReadyToExecuteTestCase(
    val testCaseData: TestCaseData,
    val injector: Injector
) : TestCaseState

internal data class PassedTestCase(
    override val testCaseData: TestCaseData,
    val injector: Injector
) :
    RegisteredTestCaseState

internal data class PostparedPassedTestCase(override val testCaseData: TestCaseData) : TestCaseEndState

internal data class FailedTestCase(
    override val testCaseData: TestCaseData,
    val injector: Injector,
    var cause: Throwable
) :
    RegisteredTestCaseState

object PostparedFailedTestCase : TestCaseState

internal data class SkippedTestCase(
    override val testCaseData: TestCaseData,
    val reason: String
) : TestCaseEndState

internal interface TestCaseMessage
internal data class RegisterTestCase(val testCaseData: TestCaseData) : TestCaseMessage

internal data class SkipTestCase(val reason: String) : TestCaseMessage
internal data class PrepareTestCase(val parentInjector: Injector) : TestCaseMessage
internal data class TestCaseFailed(val cause: Throwable) : TestCaseMessage
internal object TestCasePassed : TestCaseMessage
internal object PostpareTestCase : TestCaseMessage

internal data class CanProvideParameter(
    val dependencyType: Class<*>,
    val result: CompletableDeferred<Boolean> = CompletableDeferred()
) : TestCaseMessage

internal data class ResolveParameter(
    val dependencyType: Class<*>,
    val result: CompletableDeferred<Any> = CompletableDeferred()
) : TestCaseMessage
