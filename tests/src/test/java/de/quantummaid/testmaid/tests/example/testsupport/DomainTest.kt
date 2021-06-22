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

package de.quantummaid.testmaid.tests.example.testsupport

import de.quantummaid.injectmaid.InjectMaid
import de.quantummaid.injectmaid.api.InjectorConfiguration
import de.quantummaid.testmaid.TestMaid
import de.quantummaid.testmaid.junit5.TestMaidJunit5Adapter
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@ExtendWith(DomainTestSupport::class)
annotation class DomainTest(vararg val injectorConfigurations: KClass<out InjectorConfiguration>)

private class DomainTestSupport : TestMaidJunit5Adapter(testMaid) {
    companion object {
        private val testMaid = testMaid()

        fun testMaid(): TestMaid {
            val injectMaidBuilder = InjectMaid.anInjectMaid()
                .withLifecycleManagement()
                .closeOnJvmShutdown()
                .withConfiguration(InMemoryConfiguration())
            return TestMaid.craftTestMaid(injectMaidBuilder)
        }
    }
}
