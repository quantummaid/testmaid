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
import de.quantummaid.testmaid.tests.example.usecases.CustomerRepository
import java.util.*

class CustomerRepositoryInMemory : CustomerRepository {
    private val db = mutableMapOf<UUID, Customer>()

    override fun create(firstName: String, lastName: String): Customer {
        val customer = Customer(UUID.randomUUID(), firstName, lastName)
        db[customer.customerId] = customer
        return customer
    }

    override fun listCustomers(): List<Customer> {
        return db.values.map { it.copy() }.toList()
    }

    override fun delete(customer: Customer) {
        db.remove(customer.customerId)
    }
}
