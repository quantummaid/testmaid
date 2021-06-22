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
import de.quantummaid.testmaid.LifecycleListener
import de.quantummaid.testmaid.SkipDecider
import de.quantummaid.testmaid.internal.StateMachineActor
import de.quantummaid.testmaid.internal.StateMachineBuilder.Companion.aStateMachineUsing
import de.quantummaid.testmaid.internal.testclass.TestClassActor
import de.quantummaid.testmaid.model.Timings
import de.quantummaid.testmaid.model.testcase.TestCaseData
import de.quantummaid.testmaid.model.testclass.TestClass
import de.quantummaid.testmaid.model.testclass.TestClassData
import de.quantummaid.testmaid.model.testsuite.TestSuiteScope
import kotlinx.coroutines.CompletableDeferred

internal fun testSuiteStateMachine(): StateMachineActor<TestSuiteState, TestSuiteMessage> {
    return testSuiteStateMachineBuilder.build()
}

private val testSuiteStateMachineBuilder = aStateMachineUsing(TestSuiteState::class, TestSuiteMessage::class)
    .withInitialState(Initial())
    .withEndStateSuperClass(TestSuiteEndState::class)
    .withQueryHandler(InitializedTestSuiteState::class, QueryTimings::class) {
        it.timings.complete(timings)
    }
    .withTransition(Initial::class, PrepareTestSuite::class) {
        val scopedInjector = it.parentInjector.enterScope(TestSuiteScope())
        it.lifecycleListener.beforeTestSuite(scopedInjector)
        ReadyToExecute(timings.recordRegisteredNow(), scopedInjector, it.lifecycleListener, it.skipDecider)
    }
    .withTransition(ReadyToExecute::class, RegisterTestClass::class) {
        val testClassActor = TestClassActor.aTestClassActor()
        testClasses[it.testClassData] = testClassActor
        testClassActor.register(it.testClassData)

        val executionDecision = skipDecider.skipTestClass(it.testClassData, injector)
        if (!executionDecision.execute) {
            testClassActor.skip(executionDecision.reason)
        }
        it.executionDecision.complete(executionDecision)
        this
    }
    .withTransition(ReadyToExecute::class, PrepareTestClass::class) {
        val testClassActor = testClasses[it.testClassData]!!
        testClassActor.prepare(it.testClassData, injector)
        this
    }
    .withTransition(ReadyToExecute::class, PostpareTestClass::class) {
        val testClassActor = testClasses[it.testClassData]!!
        testClassActor.postpare()
        this
    }
    .withTransition(ReadyToExecute::class, CanProvideTestClassParameter::class) {
        val testClassActor = testClasses[it.testClassData]!!

        val canProvide = testClassActor.canProvideDependency(it.dependencyType)
        it.result.complete(canProvide)
        this
    }
    .withTransition(ReadyToExecute::class, CanProvideTestCaseParameter::class) {
        val testClassActor = testClasses[it.testCaseData.testClassData]!!

        val canProvide = testClassActor.canProvideTestCaseDependency(it.testCaseData, it.dependencyType)
        it.result.complete(canProvide)
        this
    }
    .withTransition(ReadyToExecute::class, ResolveTestClassParameter::class) {
        val testClassActor = testClasses[it.testClassData]!!

        val resolution = testClassActor.resolveDependency(it.dependencyType)
        it.result.complete(resolution)
        this
    }
    .withTransition(ReadyToExecute::class, ResolveTestCaseParameter::class) {
        val testClassActor = testClasses[it.testCaseData.testClassData]!!

        val canProvide = testClassActor.resolveTestCaseDependency(it.testCaseData, it.dependencyType)
        it.result.complete(canProvide)
        this
    }

    .withTransition(ReadyToExecute::class, RegisterTestCase::class) {
        val testClassActor = testClasses[it.testCaseData.testClassData]!!
        testClassActor.registerTestCase(it.testCaseData)
        val executionDecision = skipDecider.skipTestCase(it.testCaseData, injector)
        if (!executionDecision.execute) {
            testClassActor.skipTestCase(it.testCaseData, executionDecision.reason)
        }
        it.executionDecision.complete(executionDecision)
        this
    }
    .withTransition(ReadyToExecute::class, PrepareTestCase::class) {
        val testClassActor = testClasses[it.testCaseData.testClassData]!!
        testClassActor.prepareTestCase(it.testCaseData)
        this
    }
    .withTransition(ReadyToExecute::class, PostpareTestCase::class) {
        val testClassActor = testClasses[it.testCaseData.testClassData]!!
        testClassActor.postpareTestCase(it.testCaseData, it.error)
        this
    }
    .withTransition(ReadyToExecute::class, AllTestsFinished::class) {
        Failed(timings.recordExecutedNow(), injector, lifecycleListener, testClasses)
        Passed(timings.recordExecutedNow(), injector, lifecycleListener, testClasses)
    }

internal interface TestSuiteState {
    val timings: Timings
}

internal interface InitializedTestSuiteState : TestSuiteState {
    val injector: Injector
    val lifecycleListener: LifecycleListener
    val testClasses: Map<TestClassData, TestClass>
}

internal interface TestSuiteEndState : InitializedTestSuiteState

internal data class Initial(override val timings: Timings = Timings()) : TestSuiteState

internal data class ReadyToExecute(
    override val timings: Timings,
    override val injector: Injector,
    override val lifecycleListener: LifecycleListener,
    val skipDecider: SkipDecider,
    override val testClasses: MutableMap<TestClassData, TestClass> = mutableMapOf()

) : InitializedTestSuiteState

internal data class Passed(
    override val timings: Timings,
    override val injector: Injector,
    override val lifecycleListener: LifecycleListener,
    override val testClasses: Map<TestClassData, TestClass>
) :
    InitializedTestSuiteState

internal data class Failed(
    override val timings: Timings,
    override val injector: Injector,
    override val lifecycleListener: LifecycleListener,
    override val testClasses: Map<TestClassData, TestClass>
) : InitializedTestSuiteState

internal interface TestSuiteMessage
internal data class QueryTimings(val timings: CompletableDeferred<Timings> = CompletableDeferred()) : TestSuiteMessage
internal data class PrepareTestSuite(
    val parentInjector: Injector,
    val skipDecider: SkipDecider,
    val lifecycleListener: LifecycleListener
) : TestSuiteMessage

internal data class RegisterTestClass(
    val testClassData: TestClassData,
    val executionDecision: CompletableDeferred<ExecutionDecision> = CompletableDeferred()
) : TestSuiteMessage

internal data class PrepareTestClass(val testClassData: TestClassData) : TestSuiteMessage
internal data class PostpareTestClass(val testClassData: TestClassData) : TestSuiteMessage
internal data class RegisterTestCase(
    val testCaseData: TestCaseData,
    val executionDecision: CompletableDeferred<ExecutionDecision> = CompletableDeferred()
) : TestSuiteMessage

internal data class PrepareTestCase(val testCaseData: TestCaseData) : TestSuiteMessage
internal data class PostpareTestCase(val testCaseData: TestCaseData, val error: Throwable?) : TestSuiteMessage
internal object AllTestsFinished : TestSuiteMessage
internal data class CanProvideTestClassParameter(
    val testClassData: TestClassData,
    val dependencyType: Class<Any>,
    val result: CompletableDeferred<Boolean> = CompletableDeferred()
) : TestSuiteMessage

internal data class CanProvideTestCaseParameter(
    val testCaseData: TestCaseData,
    val dependencyType: Class<Any>,
    val result: CompletableDeferred<Boolean> = CompletableDeferred()
) : TestSuiteMessage


internal data class ResolveTestClassParameter(
    val testClassData: TestClassData,
    val dependencyType: Class<Any>,
    val result: CompletableDeferred<Any> = CompletableDeferred()
) : TestSuiteMessage

internal data class ResolveTestCaseParameter(
    val testCaseData: TestCaseData,
    val dependencyType: Class<Any>,
    val result: CompletableDeferred<Any> = CompletableDeferred()
) : TestSuiteMessage
