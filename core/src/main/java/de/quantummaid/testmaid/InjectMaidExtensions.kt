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

package de.quantummaid.testmaid

import de.quantummaid.injectmaid.InjectMaidBuilder
import de.quantummaid.injectmaid.api.ReusePolicy
import de.quantummaid.testmaid.model.testcase.TestCaseData
import de.quantummaid.testmaid.model.testcase.TestCaseScope
import de.quantummaid.testmaid.model.testclass.TestClassScope
import de.quantummaid.testmaid.model.testsuite.TestSuiteScope

fun InjectMaidBuilder.withinTestCaseScope(block: InjectMaidBuilder.() -> Unit): InjectMaidBuilder {
    this.withinTestClassScope {
        this.withScope(TestCaseScope::class.java) {
            it.withCustomType(
                    TestCaseData::class.java,
                    TestCaseScope::class.java,
                    { bla -> bla.testCaseData },
                    ReusePolicy.DEFAULT_SINGLETON)
            block(it)
        }
    }
    return this
}

fun InjectMaidBuilder.withinTestClassScope(block: InjectMaidBuilder.() -> Unit): InjectMaidBuilder {
    this.withinTestSuiteScope {
        this.withScope(TestClassScope::class.java) {
            block(it)
        }
    }
    return this
}

fun InjectMaidBuilder.withinTestSuiteScope(block: InjectMaidBuilder.() -> Unit): InjectMaidBuilder {
    this.withScope(TestSuiteScope::class.java) {
        block(it)
    }
    return this
}

