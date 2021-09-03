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

import de.quantummaid.injectmaid.InjectMaidBuilder
import de.quantummaid.injectmaid.api.InjectorConfiguration
import de.quantummaid.injectmaid.api.ReusePolicy
import de.quantummaid.testmaid.tests.example.fixtures.*
import de.quantummaid.testmaid.tests.example.testsupport.infra.mem.CustomerRepositoryInMemory
import de.quantummaid.testmaid.tests.example.testsupport.infra.mem.SystemUnderTestInMemory
import de.quantummaid.testmaid.tests.example.testsupport.infra.mem.UseCaseLogFacadeInMemory
import de.quantummaid.testmaid.tests.example.usecases.CustomerRepository
import de.quantummaid.testmaid.tests.example.usecases.UseCaseLogFacade
import de.quantummaid.testmaid.withinTestCaseScope

class InMemoryConfiguration : InjectorConfiguration {
    override fun apply(builder: InjectMaidBuilder) {
        builder.withinTestCaseScope {
            withImplementation(
                SystemUnderTest::class.java,
                SystemUnderTestInMemory::class.java,
                ReusePolicy.DEFAULT_SINGLETON
            )
            withImplementation(
                UseCaseLogFacade::class.java,
                UseCaseLogFacadeInMemory::class.java,
                ReusePolicy.DEFAULT_SINGLETON
            )
            withImplementation(
                CustomerRepository::class.java,
                CustomerRepositoryInMemory::class.java,
                ReusePolicy.DEFAULT_SINGLETON
            )
            withType(ExistingCustomers::class.java, ReusePolicy.DEFAULT_SINGLETON)
            withType(LongInitTimeFixture::class.java, ReusePolicy.PROTOTYPE)
            withType(LongCleanupTimeFixture::class.java, ReusePolicy.PROTOTYPE)
            withType(LongInitAndCleanupTimeFixture::class.java, ReusePolicy.PROTOTYPE)
            withType(TooLongInitTimeFixture::class.java, ReusePolicy.PROTOTYPE)
            withType(TooLongCleanupTimeFixture::class.java, ReusePolicy.PROTOTYPE)
            withType(TooLongInitAndCleanupTimeFixture::class.java, ReusePolicy.PROTOTYPE)
        }
    }
}
