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

import de.quantummaid.testmaid.tests.example.fixtures.ExistingCustomers
import de.quantummaid.testmaid.tests.example.testsupport.DomainTest
import de.quantummaid.testmaid.tests.example.testsupport.SystemUnderTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@DomainTest
class CustomerSpecs {

    @Test
    fun simpleTestCase() {
        assertEquals(33, 33)
    }

    @Test
    fun testList(systemUnderTest: SystemUnderTest, existingCustomers: ExistingCustomers) {
        val result = systemUnderTest.listCustomers()
        assertEquals(existingCustomers.indianaJones().customerId, result[0].customerId)
        assertEquals(existingCustomers.indianaJones().firstName, result[0].firstName)
        assertEquals(existingCustomers.indianaJones().lastName, result[0].lastName)
        assertEquals(existingCustomers.jeanLucPicard().customerId, result[1].customerId)
        assertEquals(existingCustomers.jeanLucPicard().firstName, result[1].firstName)
        assertEquals(existingCustomers.jeanLucPicard().lastName, result[1].lastName)
        assertEquals(existingCustomers.ellenRipley().customerId, result[2].customerId)
        assertEquals(existingCustomers.ellenRipley().firstName, result[2].firstName)
        assertEquals(existingCustomers.ellenRipley().lastName, result[2].lastName)
    }
}
