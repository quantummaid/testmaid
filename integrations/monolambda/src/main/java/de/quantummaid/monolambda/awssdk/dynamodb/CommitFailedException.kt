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

import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException

class CommitFailedException(val failedActions: Collection<FailedAction>) : Exception(
        "Commit failed because of the following actions: ${failedActions}"
) {
    companion object {
        fun commitFailed(e: TransactionCanceledException, descriptions: List<String>): CommitFailedException {
            val cancellationReasons = e.cancellationReasons()
            val failedItems = cancellationReasons.mapIndexed { index, cancellationReason ->
                val code = cancellationReason.code()
                val message = cancellationReason.message()
                if (message == null && code == "NONE") {
                    null
                } else {
                    val description = descriptions[index]
                    val item = cancellationReason.item()
                    FailedAction(description, code, message, item)
                }
            }.filterNotNull()
            return CommitFailedException(failedItems)
        }
    }
}

data class FailedAction(val description: String,
                        val code: String,
                        val message: String,
                        val item: MutableMap<String, AttributeValue>?)
