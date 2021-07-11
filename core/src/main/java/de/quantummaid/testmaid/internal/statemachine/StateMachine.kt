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

package de.quantummaid.testmaid.internal.statemachine

import kotlinx.coroutines.CompletableDeferred
import kotlin.reflect.KClass

data class StateMachineMessage<T>(
    val payload: T,
    val exception: CompletableDeferred<Throwable?> = CompletableDeferred()
)

internal class StateMachine<StateSuperClass : Any, MessageSuperClass : Any>(
    private val transitions: Set<Transition<StateSuperClass, MessageSuperClass, *, *, *>>,
    private val queries: Set<Query<StateSuperClass, MessageSuperClass, *, *>>,
    initialState: StateSuperClass,
    private val endStateSuperClass: KClass<out StateSuperClass>
) {
    private var currentState = initialState

    fun handle(message: StateMachineMessage<MessageSuperClass>) {
        val newState: StateSuperClass? = transitions
            .mapNotNull {
                it.handle(currentState, message.payload)
            }
            .singleOrNull()
        if (newState != null) {
            currentState = newState
            message.exception.complete(null)
        } else {
            val query = queries.filter { it.handle(currentState, message.payload) }
            when {
                query.size == 1 -> {
                    message.exception.complete(null)
                }
                query.size > 1 -> {
                    throw UnsupportedOperationException("Multiple handlers of ${message.payload} in state ${currentState}")
                }
                else -> {
                    throw UnsupportedOperationException("Unsupported message ${message.payload::class.java} in state ${currentState::class}")
                        .initCause(null)
                }
            }
        }
    }

    fun isInEndState(): Boolean {
        return endStateSuperClass.isInstance(currentState)
    }

    fun render(): List<String> {
        val lines = mutableListOf<String>()
        transitions
            .flatMap { listOf(it.originStateClass, it.targetStateClass) }
            .distinct()
            .map {
                val name = it.simpleName
                if (endStateSuperClass.java.isAssignableFrom(it.java)) {
                    "$name [ shape=doublecircle ];"
                } else {
                    "$name [ shape=circle ];"
                }
            }
            .forEach { lines.add(it) }
        transitions
            .map {
                val from = it.originStateClass.simpleName
                val to = it.targetStateClass.simpleName
                val label = it.messageClass.simpleName
                "$from -> $to  [ label=\"$label\" ];"
            }
            .forEach { lines.add(it) }
        return lines
    }
}

