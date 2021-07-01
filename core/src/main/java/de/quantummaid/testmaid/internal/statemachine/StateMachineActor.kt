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

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking

internal class StateMachineActor<StateSuperClass : Any, MessageSuperClass : Any> private constructor(
    private val stateMachine: StateMachine<StateSuperClass, MessageSuperClass>,
    private val channel: Channel<StateMachineMessage<MessageSuperClass>>,
) : AutoCloseable {
    private lateinit var job: Job

    companion object {
        fun <State : Any, Message : Any> launch(
            actorPool: StateMachineActorPool,
            stateMachine: StateMachine<State, Message>
        ): StateMachineActor<State, Message> {
            val channel: Channel<StateMachineMessage<Message>> = Channel()
            val stateMachineActor = StateMachineActor(stateMachine, channel)
            val job = actorPool.launch {
                stateMachineActor.handleMessagesOnChannel()
            }
            stateMachineActor.job = job
            return stateMachineActor
        }
    }

    private suspend fun handleMessagesOnChannel() {
        for (msg in channel) {
            stateMachine.handle(msg)
        }
    }

    fun signalAwaitingSuccess(msg: MessageSuperClass) {
        val exception = runBlocking {
            val stateMachineMessage = StateMachineMessage(msg)
            channel.send(stateMachineMessage)
            stateMachineMessage.exception.await()
        }
        if (exception != null) {
            throw exception
        }
    }

    fun isActive(): Boolean {
        return job.isActive
    }

    fun isInEndState(): Boolean {
        return stateMachine.isInEndState()
    }

    fun stop() {
        channel.close()
    }

    override fun close() {
        stop()
    }

    fun renderStateMachine() = stateMachine.render()
}
