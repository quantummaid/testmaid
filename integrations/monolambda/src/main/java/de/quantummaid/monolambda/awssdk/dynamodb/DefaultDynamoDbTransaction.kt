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

package de.quantummaid.monolambda.awssdk.dynamodb

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.Delete
import software.amazon.awssdk.services.dynamodb.model.Put
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem

/**
 * This implementation is not thread safe. As I'm not aware of ways to make this thread safe without introducing
 * performance penalties and it is intended to run in a lambda.
 * This is fine, since:
 * A lambda is, with regards to transactions, single threaded when considering Outbox guarded CRUD Actions and Actors.
 */
class DefaultDynamoDbTransaction(override val client: DynamoDbClient) : DynamoDbTransaction {
    private var open = true
    private val txWriteItems: MutableList<TransactWriteItem> = mutableListOf()

    override fun put(block: Put.Builder.() -> Unit) {
        ensureOpen()
        val transactWriteItem = TransactWriteItem.builder()
                .put { builder -> block(builder) }
                .build()
        txWriteItems.add(transactWriteItem)
    }

    override fun delete(block: Delete.Builder.() -> Unit) {
        ensureOpen()
        val transactWriteItem = TransactWriteItem.builder()
                .delete { builder -> block(builder) }
                .build()
        txWriteItems.add(transactWriteItem)
    }

    /**
     * All exceptions caused by individual parts/txWriteItems will be thrown here. I did not find a way to determine
     * which txWriteItem's conditionCheck caused a TransactionCancelledException, since one cannot name conditions
     * in dynamodb.
     *
     * That means a TransactionCancelledException is thrown, for now, for:
     * Inserts: When using check conditions to make sure a PUT is CREATING a NEW entry, the item already exists
     * Updates: When using check conditions to implement MvccIDs, the item has been modified in the meantime
     * Delete: When using check conditions to implement MvccIDs, the item has been modified in the meantime
     */
    override fun commit() {
        ensureOpen()
        open = false
        this.client.transactWriteItems {
            it.transactItems(txWriteItems)
        }
    }

    private fun ensureOpen() {
        if (!open) {
            throw IllegalStateException("Transaction already closed")
        }
    }
}