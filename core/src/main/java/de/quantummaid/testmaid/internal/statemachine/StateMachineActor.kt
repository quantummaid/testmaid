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

import de.quantummaid.reflectmaid.actors.Actor
import de.quantummaid.reflectmaid.actors.ActorBuilder
import de.quantummaid.reflectmaid.actors.ActorPool
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object CloseMessage

internal class StateMachineActor<StateSuperClass : Any, MessageSuperClass : Any> private constructor(
    private val stateMachine: StateMachine<StateSuperClass, MessageSuperClass>,
    private val actor: Actor<StateMachine<StateSuperClass, MessageSuperClass>, Any>,
    val name: String,
) : AutoCloseable {

    companion object {
        fun <State : Any, Message : Any> launch(
            name: String,
            actorPool: ActorPool,
            stateMachine: StateMachine<State, Message>
        ): StateMachineActor<State, Message> {
            val actor: Actor<StateMachine<State, Message>, Any> =
                ActorBuilder<StateMachine<State, Message>, Any>(name)
                    .withPool(actorPool)
                    .withMutatingHandler<StateMachineMessage<Message>> { handle(it) }
                    .closeOn<CloseMessage>()
                    .withInitialState(stateMachine)
                    .launch()
            return StateMachineActor(stateMachine, actor, name)
        }
    }

    fun signalAwaitingSuccess(
        msg: MessageSuperClass,
        timeout: Duration = seconds(10),
        exceptionWrapperFactory: (AwaitingSuccessMessageTimeoutException) -> Throwable = { it }
    ) {
        val stateMachineMessage = StateMachineMessage(msg)
        try {
            actor.signalAwaitingSuccess(stateMachineMessage, timeout)
        } catch (e: TimeoutCancellationException) {
            throw exceptionWrapperFactory(AwaitingSuccessMessageTimeoutException(actor.name, msg, timeout, e))
        }
    }

    fun isActive(): Boolean {
        return actor.isActive()
    }

    fun isInEndState(): Boolean {
        return stateMachine.isInEndState()
    }

    fun stop() {
        actor.signalAwaitingSuccess(CloseMessage)
    }

    override fun close() {
        stop()
    }

    fun renderStateMachine() = stateMachine.render()
}
