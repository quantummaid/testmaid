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

package de.quantummaid.testmaid.tests.example

import de.quantummaid.injectmaid.InjectMaid
import de.quantummaid.testmaid.TestMaid
import de.quantummaid.testmaid.junit5.TestMaidJunit5Adapter
import de.quantummaid.testmaid.model.testcase.TestCaseScope
import de.quantummaid.testmaid.model.testclass.TestClassScope
import de.quantummaid.testmaid.model.testsuite.TestSuiteScope
import de.quantummaid.testmaid.tests.example.testsupport.InMemoryConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@ExtendWith(TechnicalTestSupport::class)
annotation class TechnicalTest

class FailingAutoclosableInTestCaseScope : AutoCloseable {

    override fun close() {
        throw UnsupportedOperationException()
    }
}

class FailingAutoclosableInTestClassScope : AutoCloseable {

    override fun close() {
        throw UnsupportedOperationException()
    }
}

class FailingAutoclosableInTestSuiteScope : AutoCloseable {

    override fun close() {
        throw UnsupportedOperationException()
    }
}

class FailingAutoclosableInGlobalScope : AutoCloseable {

    override fun close() {
        throw UnsupportedOperationException()
    }
}

data class SomethingThatDependsOnTestSuiteScope(val scope: TestSuiteScope)
data class SomethingThatDependsOnTestClassScope(val scope: TestClassScope)
data class SomethingThatDependsOnTestCaseScope(val scope: TestCaseScope)

private class TechnicalTestSupport : TestMaidJunit5Adapter(testMaid) {
    companion object {
        private val testMaid = testMaid()

        fun testMaid(): TestMaid {
            val injectMaidBuilder = InjectMaid.anInjectMaid()
                .withLifecycleManagement()
                .closeOnJvmShutdown()
                .withType(FailingAutoclosableInGlobalScope::class.java)
                .withScope(TestSuiteScope::class.java) { testSuiteInjectMaid ->
                    testSuiteInjectMaid.withType(FailingAutoclosableInTestSuiteScope::class.java)

                    testSuiteInjectMaid.withCustomType(
                        SomethingThatDependsOnTestSuiteScope::class.java,
                        TestSuiteScope::class.java
                    ) { SomethingThatDependsOnTestSuiteScope(it) }

                    testSuiteInjectMaid.withScope(TestClassScope::class.java) { testClassInjectMaid ->
                        testClassInjectMaid.withType(FailingAutoclosableInTestClassScope::class.java)

                        testClassInjectMaid.withCustomType(
                            SomethingThatDependsOnTestClassScope::class.java,
                            TestClassScope::class.java
                        ) { SomethingThatDependsOnTestClassScope(it) }

                        testClassInjectMaid.withScope(TestCaseScope::class.java) { testCaseInjectMaid ->
                            testCaseInjectMaid.withType(FailingAutoclosableInTestCaseScope::class.java)

                            testClassInjectMaid.withCustomType(
                                SomethingThatDependsOnTestCaseScope::class.java,
                                TestCaseScope::class.java
                            ) { SomethingThatDependsOnTestCaseScope(it) }
                        }
                    }
                }
                .withConfiguration(InMemoryConfiguration())
            val testMaid = TestMaid.buildTestMaid(injectMaidBuilder)
            testMaid.injectionApi
            return testMaid
        }
    }
}

@TechnicalTest
class FailsOnTestCaseInjectorCloseSpecs0 {

    @Disabled
    @Test
    fun testList(failingAutoclosableInTestCaseScope: FailingAutoclosableInTestCaseScope) {
        Assertions.assertNotNull(failingAutoclosableInTestCaseScope)
    }
}

@TechnicalTest
class FailsOnTestClassInjectorCloseSpecs1 {

    @Disabled
    @Test
    fun testList(failingAutoclosableInTestClassScope: FailingAutoclosableInTestClassScope) {
        Assertions.assertNotNull(failingAutoclosableInTestClassScope)
    }
}

@TechnicalTest
class FailsOnTestSuiteInjectorCloseSpecs2 {

    @Disabled
    @Test
    fun testList(failingAutoclosableInTestSuiteScope: FailingAutoclosableInTestSuiteScope) {
        Assertions.assertNotNull(failingAutoclosableInTestSuiteScope)
    }
}

@TechnicalTest
class FailsOnGlobalInjectorCloseSpecs3 {

    @Disabled
    @Test
    fun testList(failingAutoclosableInGlobalScope: FailingAutoclosableInGlobalScope) {
        Assertions.assertNotNull(failingAutoclosableInGlobalScope)
    }
}

@TechnicalTest
class UsesAllThreeTestScopesAsTestClass {
    @Test
    fun usesAllThreeTestScopes(
        dependsOnSuiteScope: SomethingThatDependsOnTestSuiteScope,
        dependsOnClassScope: SomethingThatDependsOnTestClassScope,
        dependsOnTestScope: SomethingThatDependsOnTestCaseScope
    ) {
        assertEquals("TBD", dependsOnClassScope.scope.testClassData.name)
        assertEquals("TBD", dependsOnTestScope.scope.testCaseData.name)
    }
}

interface UsesAllThreeTestScopesAsTestInterface {
    @Test
    @Tag("stephanewashere")
    fun usesAllThreeTestScopes(
        dependsOnSuiteScope: SomethingThatDependsOnTestSuiteScope,
        dependsOnClassScope: SomethingThatDependsOnTestClassScope,
        dependsOnTestScope: SomethingThatDependsOnTestCaseScope
    ) {
        assertEquals("TBD", dependsOnClassScope.scope.testClassData.name)
        assertEquals("TBD", dependsOnTestScope.scope.testCaseData.name)
    }
}

@TechnicalTest
class UsesAllThreeTestScopesThroughInterface: UsesAllThreeTestScopesAsTestInterface {}
