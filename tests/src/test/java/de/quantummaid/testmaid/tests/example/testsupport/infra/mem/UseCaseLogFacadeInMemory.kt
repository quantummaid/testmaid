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

package de.quantummaid.testmaid.tests.example.testsupport.infra.mem

import de.quantummaid.testmaid.tests.example.model.Customer
import de.quantummaid.testmaid.tests.example.usecases.UseCaseLogFacade

class UseCaseLogFacadeInMemory : UseCaseLogFacade {
    private val loggedStatements = mutableListOf<String>()

    override fun debugCreatingCustomer(firstName: String, lastName: String) {
        loggedStatements.add("Creating customer ${firstName} ${lastName}")
    }

    override fun infoCustomerCreated(customer: Customer) {
        loggedStatements.add("Customer created ${customer.customerId} ${customer.firstName} ${customer.lastName}")
    }

    override fun debugListingCustomers(customers: List<Customer>) {
        val joined = customers.joinToString { "${it.customerId} ${it.firstName} ${it.lastName}" }
        loggedStatements.add("listing customers ${joined}")
    }
}
