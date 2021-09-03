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

package de.quantummaid.testmaid.junit5

import de.quantummaid.testmaid.ExecutionDecision
import de.quantummaid.testmaid.TestMaid
import de.quantummaid.testmaid.model.testcase.TestCaseData
import de.quantummaid.testmaid.model.testclass.TestClassData
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.*
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock


abstract class TestMaidJunit5Adapter(val testMaid: TestMaid) :
    BeforeAllCallback,
    AfterAllCallback,
    BeforeTestExecutionCallback,
    AfterTestExecutionCallback,
    ExecutionCondition,
    ParameterResolver,
    ExtensionContext.Store.CloseableResource {

    companion object {
        private val lock: Lock = ReentrantLock()
        private var started = false
    }

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val testClass: Class<*> = context.requiredTestClass
        val name = context.fullyQualifiedName()
        val decision = when {
            context.isTestClass() -> {
                val testClassData = TestClassData(name, testClass, context.tags)
                context.putInMyStore(context.uniqueId, testClassData)
                context.putInMyStore(testClass.canonicalName, testClassData)
                testMaid.integrationApi.registerTestClass(testClassData)
            }
            context.isTestCase() -> {
                val testMethod = context.requiredTestMethod
                val testClassData = context.getFromMyStore(testClass.canonicalName, TestClassData::class.java)
                val testCaseData = TestCaseData(name, testMethod, context.tags, testClassData)
                context.putInMyStore(context.uniqueId, testCaseData)
                testMaid.integrationApi.registerTestCase(testCaseData)
            }
            else -> {
                ExecutionDecision(
                    true, """
                        Never skip method based parents of test cases. This decision is forced when using things like 
                        @RepeatedTest(3).
                        In that case, evaluateExecutionCondition is called 4 times, 3 times for each actual test case and 
                        once for the test method, which is the parent context/scope of the actual test cases.
                    """.trimIndent()
                )
            }
        }
        return if (decision.execute) {
            ConditionEvaluationResult.enabled(decision.reason)
        } else {
            ConditionEvaluationResult.disabled(decision.reason)
        }
    }

    override fun beforeTestExecution(context: ExtensionContext) {
        val testCaseData = context.getFromMyStore(context.uniqueId, TestCaseData::class.java)
        testMaid.integrationApi.testCaseStart(testCaseData)
    }

    override fun afterTestExecution(context: ExtensionContext) {
        val testCaseData = context.getFromMyStore(context.uniqueId, TestCaseData::class.java)
        testMaid.integrationApi.testCaseFinish(testCaseData, context.executionException.orElse(null))
    }

    override fun beforeAll(context: ExtensionContext) {
        lock.lock()
        try {
            if (!started) {
                started = true;
                context.root.getStore(GLOBAL).put("afterAllHook", this);
            }
        } finally {
            lock.unlock()
        }
        val testClassData = context.getFromMyStore(context.uniqueId, TestClassData::class.java)
        testMaid.integrationApi.testClassStart(testClassData)
    }

    override fun afterAll(context: ExtensionContext) {
        val testClassData = context.getFromMyStore(context.uniqueId, TestClassData::class.java)
        testMaid.integrationApi.testClassFinish(testClassData)
    }

    override fun close() {
        testMaid.integrationApi.testSuiteFinish()
        testMaid.close()
    }

    override fun supportsParameter(parameterContext: ParameterContext, context: ExtensionContext): Boolean {
        val parameterSupported = when {
            context.isTestClass() -> {
                val testClassData = context.getFromMyStore(context.uniqueId, TestClassData::class.java)
                testMaid.injectionApi.canProvideTestClassDependency(
                    testClassData, parameterContext.parameter.type as Class<Any>
                )
            }
            context.isTestCase() -> {
                val testCaseData = context.getFromMyStore(context.uniqueId, TestCaseData::class.java)
                testMaid.injectionApi.canProvideTestCaseDependency(
                    testCaseData,
                    parameterContext.parameter.type as Class<Any>
                )
            }
            else -> {
                throw UnsupportedOperationException(
                    "Could not decide whether this is a parameter of a test case or test class"
                )
            }
        }
        return parameterSupported
    }

    override fun resolveParameter(parameterContext: ParameterContext, context: ExtensionContext): Any {
        val parameter = when {
            context.isTestClass() -> {
                val testClassData = context.getFromMyStore(context.uniqueId, TestClassData::class.java)
                testMaid.injectionApi.resolveTestClassDependency(
                    testClassData, parameterContext.parameter.type as Class<Any>
                )
            }
            context.isTestCase() -> {
                val testCaseData = context.getFromMyStore(context.uniqueId, TestCaseData::class.java)
                testMaid.injectionApi.resolveTestCaseDependency(
                    testCaseData,
                    parameterContext.parameter.type as Class<Any>
                )
            }
            else -> {
                throw UnsupportedOperationException(
                    "Could not decide whether this is a parameter of a test case or test class"
                )
            }
        }
        return parameter
    }
}

private fun ExtensionContext.putInMyStore(key: String, value: Any) {
    myStore().put(key, value)
}

private fun <T : Any> ExtensionContext.getFromMyStore(key: String, type: Class<T>): T {
    return myStore().get(key, type)
}

private val namespace = Namespace.create(TestMaidJunit5Adapter::class.java.canonicalName)

private fun ExtensionContext.myStore(): ExtensionContext.Store {
    return getStore(namespace)
}

/**
 * If you have 2 different test classes and both have a method named "test1", displayName will return "test1" for both
 * classes. So this method builds a globally unique name of the test case or test class.
 */
internal fun ExtensionContext.fullyQualifiedName(includingRoot: Boolean = false, separator: String = "."): String {
    val contextPath = mutableListOf(this)
    var currentContext = this
    val root = this.root
    while (currentContext.parent.isPresent) {
        currentContext = currentContext.parent.orElseThrow()!!
        if (includingRoot || currentContext != root)
            contextPath.add(currentContext)
    }
    contextPath.reverse()
    //                  class                    method           . repeat/parameter
    //root.displayName.{context.displayName}.{context.displayName}.{context.displayName}
    return contextPath.joinToString(separator = separator) { it.displayName }
}


internal fun ExtensionContext.isTestCase(): Boolean {
    val lifecycle = testInstanceLifecycle.orElseThrow()
    if (lifecycle != TestInstance.Lifecycle.PER_METHOD) {
        throw UnsupportedOperationException(
            "Unsupported test lifecycle ${lifecycle}, only TestInstance.Lifecycle.PER_METHOD is supported"
        )
    }
    return testMethod.isPresent && testInstance.isPresent
}

internal fun ExtensionContext.isTestClass(): Boolean {
    return (!testMethod.isPresent) && testClass.isPresent
}
