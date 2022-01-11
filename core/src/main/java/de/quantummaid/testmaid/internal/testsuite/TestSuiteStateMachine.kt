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
import de.quantummaid.testmaid.Timeouts
import de.quantummaid.testmaid.internal.statemachine.StateMachineActor
import de.quantummaid.testmaid.internal.statemachine.StateMachineBuilder
import de.quantummaid.testmaid.internal.statemachine.StateMachineBuilder.Companion.aStateMachineUsing
import de.quantummaid.testmaid.internal.testclass.TestClassActor
import de.quantummaid.testmaid.model.testcase.TestCaseData
import de.quantummaid.testmaid.model.testclass.TestClass
import de.quantummaid.testmaid.model.testclass.TestClassData
import de.quantummaid.testmaid.model.testsuite.TestSuiteScope
import kotlinx.coroutines.CompletableDeferred

internal fun testSuiteStateMachine(timeouts: Timeouts): StateMachineActor<TestSuiteState, TestSuiteMessage> {
    return testSuiteStateMachineBuilder(timeouts).build()
}

private fun testSuiteStateMachineBuilder(timeouts: Timeouts): StateMachineBuilder<TestSuiteState, TestSuiteMessage> {
    return aStateMachineUsing<TestSuiteState, TestSuiteMessage>("TestSuiteActor")
        .withInitialState(Initial(timeouts))
        .withEndStateSuperClass<TestSuiteEndState>()
        .withTransition<Initial, PrepareTestSuite, ReadyToExecute> {
            val scopedInjector = it.parentInjector.enterScope(TestSuiteScope())
            ReadyToExecute(this.timeouts, scopedInjector, it.skipDecider)
        }
        .withTransition<ReadyToExecute, RegisterTestClass, ReadyToExecute> {
            val testClassActor = TestClassActor.aTestClassActor(this.timeouts)
            it.actor.complete(testClassActor.delegate)
            testClasses[it.testClassData] = testClassActor
            testClassActor.register(it.testClassData)

            val executionDecision = skipDecider.skipTestClass(it.testClassData, scopedInjector)
            if (!executionDecision.execute) {
                testClassActor.skip(executionDecision.reason)
            }
            it.executionDecision.complete(executionDecision)
            this
        }
        .withTransition<ReadyToExecute, PrepareTestClass, ReadyToExecute> {
            val testClassActor = testClasses[it.testClassData]!!
            testClassActor.prepare(it.testClassData, scopedInjector)
            this
        }
        .withTransition<ReadyToExecute, PostpareTestClass, ReadyToExecute> {
            val testClassActor = testClasses[it.testClassData]!!
            testClassActor.postpare()
            this
        }
        .withQuery<ReadyToExecute, CanProvideTestClassParameter> {
            val testClassActor = testClasses[it.testClassData]!!
            val canProvide = testClassActor.canProvideDependency(it.dependencyType)
            it.result.complete(canProvide)
        }
        .withQuery<ReadyToExecute, CanProvideTestCaseParameter> {
            val testClassActor = testClasses[it.testCaseData.testClassData]!!
            val canProvide = testClassActor.canProvideTestCaseDependency(it.testCaseData, it.dependencyType)
            it.result.complete(canProvide)
        }
        .withQuery<ReadyToExecute, ResolveTestClassParameter> {
            val testClassActor = testClasses[it.testClassData]!!
            val resolution = testClassActor.resolveDependency(it.dependencyType)
            it.result.complete(resolution)
        }
        .withQuery<ReadyToExecute, ResolveTestCaseParameter> {
            val testClassActor = testClasses[it.testCaseData.testClassData]!!
            val canProvide = testClassActor.resolveTestCaseDependency(it.testCaseData, it.dependencyType)
            it.result.complete(canProvide)
        }
        .withTransition<ReadyToExecute, RegisterTestCase, ReadyToExecute> {
            val testClassActor = testClasses[it.testCaseData.testClassData]!!
            val actor = testClassActor.registerTestCase(it.testCaseData)
            it.actor.complete(actor)
            val executionDecision = skipDecider.skipTestCase(it.testCaseData, scopedInjector)
            if (!executionDecision.execute) {
                testClassActor.skipTestCase(it.testCaseData, executionDecision.reason)
            }
            it.executionDecision.complete(executionDecision)
            this
        }
        .withTransition<ReadyToExecute, PrepareTestCase, ReadyToExecute> {
            val testClassActor = testClasses[it.testCaseData.testClassData]!!
            testClassActor.prepareTestCase(it.testCaseData)
            this
        }
        .withTransition<ReadyToExecute, PostpareTestCase, ReadyToExecute> {
            val testClassActor = testClasses[it.testCaseData.testClassData]!!
            testClassActor.postpareTestCase(it.testCaseData, it.error)
            this
        }
        .withTransition<ReadyToExecute, AllTestsFinished, Passed> {
            scopedInjector.close()
            Passed(scopedInjector, testClasses)
        }
}

internal interface TestSuiteState

internal interface InitializedTestSuiteState : TestSuiteState {
    val scopedInjector: Injector
    val testClasses: Map<TestClassData, TestClass>
}

internal interface TestSuiteEndState : InitializedTestSuiteState

internal class Initial(
    val timeouts: Timeouts
) : TestSuiteState

internal data class ReadyToExecute(
    val timeouts: Timeouts,
    override val scopedInjector: Injector,
    val skipDecider: SkipDecider,
    override val testClasses: MutableMap<TestClassData, TestClass> = mutableMapOf()

) : InitializedTestSuiteState

internal data class Passed(
    override val scopedInjector: Injector,
    override val testClasses: Map<TestClassData, TestClass>
) : InitializedTestSuiteState

internal interface TestSuiteMessage
internal data class PrepareTestSuite(
    val parentInjector: Injector,
    val skipDecider: SkipDecider,
) : TestSuiteMessage

internal data class RegisterTestClass(
    val testClassData: TestClassData,
    val actor: CompletableDeferred<AutoCloseable> = CompletableDeferred(),
    val executionDecision: CompletableDeferred<ExecutionDecision> = CompletableDeferred()
) : TestSuiteMessage

internal data class PrepareTestClass(val testClassData: TestClassData) : TestSuiteMessage
internal data class PostpareTestClass(val testClassData: TestClassData) : TestSuiteMessage
internal data class RegisterTestCase(
    val testCaseData: TestCaseData,
    val executionDecision: CompletableDeferred<ExecutionDecision> = CompletableDeferred(),
    val actor: CompletableDeferred<AutoCloseable> = CompletableDeferred()
) : TestSuiteMessage

internal data class PrepareTestCase(val testCaseData: TestCaseData) : TestSuiteMessage
internal data class PostpareTestCase(val testCaseData: TestCaseData, val error: Throwable?) : TestSuiteMessage
internal object AllTestsFinished : TestSuiteMessage

internal data class CanProvideTestClassParameter(
    val testClassData: TestClassData,
    val dependencyType: Class<*>,
    val result: CompletableDeferred<Boolean> = CompletableDeferred()
) : TestSuiteMessage

internal data class CanProvideTestCaseParameter(
    val testCaseData: TestCaseData,
    val dependencyType: Class<*>,
    val result: CompletableDeferred<Boolean> = CompletableDeferred()
) : TestSuiteMessage

internal data class ResolveTestClassParameter(
    val testClassData: TestClassData,
    val dependencyType: Class<Any>,
    val result: CompletableDeferred<Any> = CompletableDeferred()
) : TestSuiteMessage

internal data class ResolveTestCaseParameter(
    val testCaseData: TestCaseData,
    val dependencyType: Class<*>,
    val result: CompletableDeferred<Any> = CompletableDeferred()
) : TestSuiteMessage
