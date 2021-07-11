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

package de.quantummaid.testmaid.internal

import de.quantummaid.testmaid.TestMaid
import de.quantummaid.testmaid.internal.statemachine.StateMachineBuilder
import de.quantummaid.testmaid.internal.statemachine.StateMachineBuilder.Companion.aStateMachineUsing
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

interface ExampleState
class ExampleStateInitial : ExampleState
data class ExampleStateWorking(val message: String) : ExampleState
class ExampleStateEnd : ExampleState
interface ExampleStateMessage
data class InitializeExampleState(val message: String) : ExampleStateMessage
data class QueryExampleStateMessage(
    val message: CompletableDeferred<String> = CompletableDeferred()
) : ExampleStateMessage

object EndExampleState : ExampleStateMessage

private val stateMachineBuilder =
    aStateMachineUsing<ExampleState, ExampleStateMessage>("test")
        .withInitialState(ExampleStateInitial())
        .withEndStateSuperClass<ExampleStateEnd>()
        .withTransition<ExampleStateInitial, InitializeExampleState, ExampleStateWorking> {
            ExampleStateWorking(it.message)
        }
        .withQuery<ExampleStateWorking, QueryExampleStateMessage> {
            it.message.complete(message)
        }
        .withTransition<ExampleStateWorking, EndExampleState, ExampleStateEnd> {
            ExampleStateEnd()
        }

class HelperSpecs {
    @Test
    internal fun testHappyPathTransition() {
        runBlocking {
            val timeout = System.currentTimeMillis() + 10000
            while (StateMachineBuilder.actorPool.activeActors.size > 0 && System.currentTimeMillis() < timeout) {
                delay(1)
            }
            assertEquals(0, StateMachineBuilder.actorPool.activeActors.size)
        }
        assertEquals(0, StateMachineBuilder.actorPool.activeActors.size)
        val stateMachineActor = stateMachineBuilder.build()
        assertEquals(1, StateMachineBuilder.actorPool.activeActors.size)
        assertTrue(stateMachineActor.isActive())
        stateMachineActor.signalAwaitingSuccess(InitializeExampleState("Hello World"))
        val queryExampleStateMessage = QueryExampleStateMessage()
        stateMachineActor.signalAwaitingSuccess(queryExampleStateMessage)
        val actual = runBlocking { queryExampleStateMessage.message.await() }
        assertEquals("Hello World", actual)
        stateMachineActor.signalAwaitingSuccess(EndExampleState)
        assertTrue(stateMachineActor.isInEndState())
        assertTrue(stateMachineActor.isActive())
        stateMachineActor.stop()
        runBlocking {
            val timeout = System.currentTimeMillis() + 1000
            while (StateMachineBuilder.actorPool.activeActors.size == 1 && System.currentTimeMillis() < timeout) {
                delay(1)
            }
            assertEquals(0, StateMachineBuilder.actorPool.activeActors.size)
        }
        assertFalse(stateMachineActor.isActive())
    }

    @Test
    internal fun testUnsupportedMessageInCurrentState() {
        val stateMachineActor = stateMachineBuilder.build()
        stateMachineActor.signalAwaitingSuccess(InitializeExampleState("Hello World"))
        var exception: java.lang.Exception? = null
        try {
            stateMachineActor.signalAwaitingSuccess(InitializeExampleState("Hello World"))
        } catch (e: Exception) {
            exception = e
        }
        assertNotNull(exception)
    }

    @Test
    internal fun testExceptionInTransition() {
        val stateMachineBuilderWithException = aStateMachineUsing<ExampleState, ExampleStateMessage>("test")
                .withInitialState(ExampleStateInitial())
                .withEndStateSuperClass<ExampleStateEnd>()
                .withTransition<ExampleStateInitial, InitializeExampleState, ExampleStateWorking> {
                    throw UnsupportedOperationException()
                }
        val stateMachineActor = stateMachineBuilderWithException.build()
        var exception : UnsupportedOperationException? = null
        try {
            stateMachineActor.signalAwaitingSuccess(InitializeExampleState("abc"))
        } catch (e: UnsupportedOperationException) {
            exception = e
        }
        assertNotNull(exception)
    }

    @Test
    internal fun testRender() {
        val rendered = TestMaid.renderStateMachine()
        assertNotNull(rendered)
    }
}
