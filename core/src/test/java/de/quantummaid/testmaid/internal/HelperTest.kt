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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

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
    StateMachineBuilder.aStateMachineUsing(ExampleState::class, ExampleStateMessage::class)
        .withInitialState(ExampleStateInitial())
        .withEndStateSuperClass(ExampleStateEnd::class)
        .withTransition(ExampleStateInitial::class, InitializeExampleState::class) {
            ExampleStateWorking(it.message)
        }
        .withQueryHandler(ExampleStateWorking::class, QueryExampleStateMessage::class) {
            it.message.complete(message)
        }
        .withTransition(ExampleStateWorking::class, EndExampleState::class) {
            ExampleStateEnd()
        }

class HelperTest {
    @Test
    internal fun testHappyPathTransition() {
        assertEquals(0, StateMachineBuilder.actorPool.activeJobs.size)
        val stateMachineActor = stateMachineBuilder.build()
        assertEquals(1, StateMachineBuilder.actorPool.activeJobs.size)
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
            while (StateMachineBuilder.actorPool.activeJobs.size == 1 && System.currentTimeMillis() < timeout) {
                delay(1)
            }
            assertEquals(0, StateMachineBuilder.actorPool.activeJobs.size)
        }
        assertFalse(stateMachineActor.isActive())
    }

    @Test
    internal fun testUnsupportedMessageInCurrentState() {
        val stateMachineActor = stateMachineBuilder.build()
        stateMachineActor.signalAwaitingSuccess(InitializeExampleState("Hello World"))
        try {
            stateMachineActor.signalAwaitingSuccess(InitializeExampleState("Hello World"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}