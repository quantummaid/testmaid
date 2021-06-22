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
import de.quantummaid.testmaid.internal.StateMachineActor
import de.quantummaid.testmaid.internal.StateMachineBuilder.Companion.aStateMachineUsing
import de.quantummaid.testmaid.model.Timings
import de.quantummaid.testmaid.model.testcase.TestCaseData
import de.quantummaid.testmaid.model.testcase.TestCaseScope
import kotlinx.coroutines.CompletableDeferred

internal fun testCaseStateMachine(): StateMachineActor<TestCaseState, TestCaseMessage> {
    return testCaseStateMachineBuilder.build()
}

private val testCaseStateMachineBuilder = aStateMachineUsing(TestCaseState::class, TestCaseMessage::class)
    .withInitialState(InitialTestCase())
    .withEndStateSuperClass(TestCaseEndState::class)
    .withQueryHandler(RegisteredTestCaseState::class, QueryTimings::class) {
        it.timings.complete(timings)
    }
    .withTransition(InitialTestCase::class, RegisterTestCase::class) {
        RegisteredTestCase(timings.recordRegisteredNow(), it.testCaseData)
    }
    .withTransition(RegisteredTestCase::class, SkipTestCase::class) {
        SkippedTestCase(timings.recordSkippedNow(), testCaseData, it.reason)
    }
    .withTransition(RegisteredTestCase::class, PrepareTestCase::class) {
        val injector = it.parentInjector.enterScope(TestCaseScope(testCaseData))
        ReadyToExecuteTestCase(timings.recordPreparedNow(), testCaseData, injector)
    }
    .withTransition(ReadyToExecuteTestCase::class, CanProvideParameter::class) { msg ->
        val canInstantiate = injector.canInstantiate(msg.dependencyType)
        msg.result.complete(canInstantiate)

        this
    }
    .withTransition(ReadyToExecuteTestCase::class, ResolveParameter::class) {
        val resolution = injector.getInstance(it.dependencyType)
        it.result.complete(resolution)

        this
    }
    .withTransition(ReadyToExecuteTestCase::class, TestCaseFailed::class) {
        FailedTestCase(timings.recordExecutedNow(), testCaseData, injector, it.cause)
    }
    .withTransition(ReadyToExecuteTestCase::class, TestCasePassed::class) {
        PassedTestCase(timings.recordExecutedNow(), testCaseData, injector)
    }
    .withTransition(PassedTestCase::class, PostpareTestCase::class) {
        injector.close()
        //TODO: Proper end state
        this
    }
    .withTransition(FailedTestCase::class, PostpareTestCase::class) {
        injector.close()
        //TODO: Proper end state
        this
    }

internal interface TestCaseState {
    val timings: Timings
}

internal interface RegisteredTestCaseState : TestCaseState {
    val testCaseData: TestCaseData
}

internal interface TestCaseEndState : RegisteredTestCaseState

internal data class InitialTestCase(override val timings: Timings = Timings()) : TestCaseState

internal data class RegisteredTestCase(override val timings: Timings, override val testCaseData: TestCaseData) :
    RegisteredTestCaseState

internal data class ReadyToExecuteTestCase(
    override val timings: Timings,
    val testCaseData: TestCaseData,
    val injector: Injector
) : TestCaseState

internal data class PassedTestCase(
    override val timings: Timings,
    override val testCaseData: TestCaseData,
    val injector: Injector
) :
    RegisteredTestCaseState

internal data class FailedTestCase(
    override val timings: Timings,
    override val testCaseData: TestCaseData,
    val injector: Injector,
    var cause: Throwable
) :
    RegisteredTestCaseState

internal data class SkippedTestCase(
    override val timings: Timings,
    override val testCaseData: TestCaseData,
    val reason: String
) : TestCaseEndState

internal interface TestCaseMessage
internal data class QueryTimings(val timings: CompletableDeferred<Timings> = CompletableDeferred()) : TestCaseMessage
internal data class RegisterTestCase(val testCaseData: TestCaseData) : TestCaseMessage

internal data class SkipTestCase(val reason: String) : TestCaseMessage
internal data class PrepareTestCase(val parentInjector: Injector) : TestCaseMessage
internal data class TestCaseFailed(val cause: Throwable) : TestCaseMessage
internal object TestCasePassed : TestCaseMessage
internal object PostpareTestCase : TestCaseMessage

internal data class CanProvideParameter(
    val dependencyType: Class<Any>,
    val result: CompletableDeferred<Boolean> = CompletableDeferred()
) : TestCaseMessage

internal data class ResolveParameter(
    val dependencyType: Class<Any>,
    val result: CompletableDeferred<Any> = CompletableDeferred()
) : TestCaseMessage
