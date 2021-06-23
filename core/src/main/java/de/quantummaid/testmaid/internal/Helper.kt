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

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.reflect.KClass
import kotlin.reflect.cast

internal class StateMachineBuilder<StateSuperClass : Any, MessageSuperClass : Any> {
    companion object {
        val actorPool = StateMachineActorPool()

        fun <StateSuperClass : Any, MessageSuperClass : Any> aStateMachineUsing(
            stateSuperClass: KClass<StateSuperClass>,
            messageSuperClass: KClass<MessageSuperClass>
        ): StateMachineBuilder<StateSuperClass, MessageSuperClass> {
            return StateMachineBuilder()
        }
    }

    private val transitions = mutableSetOf<Transition<StateSuperClass, MessageSuperClass, *, *>>()
    private val queries = mutableSetOf<Query<StateSuperClass, MessageSuperClass, *, *>>()
    private var initialState: StateSuperClass? = null
    private var endStateSuperClass: KClass<out StateSuperClass>? = null

    fun withInitialState(initial: StateSuperClass): StateMachineBuilder<StateSuperClass, MessageSuperClass> {
        this.initialState = initial
        return this
    }

    fun withEndStateSuperClass(endStateSuperClass: KClass<out StateSuperClass>): StateMachineBuilder<StateSuperClass, MessageSuperClass> {
        this.endStateSuperClass = endStateSuperClass
        return this
    }

    fun <OriginState : StateSuperClass, Message : MessageSuperClass> withQueryHandler(
        originStateClass: KClass<OriginState>,
        messageClass: KClass<Message>,
        block: OriginState.(Message) -> Unit
    ): StateMachineBuilder<StateSuperClass, MessageSuperClass> {
        queries.add(Query(originStateClass, messageClass, block))
        return this
    }

    fun <OriginState : StateSuperClass, Message : MessageSuperClass> withTransition(
        originStateClass: KClass<OriginState>,
        messageClass: KClass<Message>,
        block: OriginState.(Message) -> StateSuperClass
    ): StateMachineBuilder<StateSuperClass, MessageSuperClass> {
        transitions.add(Transition(originStateClass, messageClass, block))
        return this
    }

    fun build(): StateMachineActor<StateSuperClass, MessageSuperClass> {
        val stateMachine: StateMachine<StateSuperClass, MessageSuperClass> = StateMachine(
            transitions, queries, initialState!!, endStateSuperClass!!
        )

        return StateMachineActor.launch(actorPool, stateMachine)
    }
}

internal class Transition<StateSuperClass : Any, MessageSuperClass : Any, OriginState : StateSuperClass, Message : MessageSuperClass>(
    val originStateClass: KClass<OriginState>,
    val messageClass: KClass<Message>,
    val handler: OriginState.(Message) -> StateSuperClass
) {

    fun handle(currentState: StateSuperClass, message: MessageSuperClass): StateSuperClass? {
        val stateMatches = currentState::class == this.originStateClass
        if (stateMatches) {
            val messageMatches = message::class == this.messageClass
            if (messageMatches) {
                val castedState = this.originStateClass.cast(currentState)
                val castedMessage = this.messageClass.cast(message)
                val newState = handler(castedState, castedMessage)
                return newState
            }
        }
        return null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Transition<*, *, *, *>) return false

        if (originStateClass != other.originStateClass) return false
        if (messageClass != other.messageClass) return false

        return true
    }

    override fun hashCode(): Int {
        var result = originStateClass.hashCode()
        result = 31 * result + messageClass.hashCode()
        return result
    }

    override fun toString(): String {
        return "Transition(originStateClass=$originStateClass, messageClass=$messageClass)"
    }
}

internal class Query<StateSuperClass : Any, MessageSuperClass : Any, OriginState : StateSuperClass, Message : MessageSuperClass>(
    val originStateClass: KClass<OriginState>,
    val messageClass: KClass<Message>,
    val handler: OriginState.(Message) -> Unit
) {

    fun handle(currentState: StateSuperClass, message: MessageSuperClass): Boolean {
        val stateMatches = this.originStateClass.isInstance(currentState)
        if (stateMatches) {
            val messageMatches = message::class == this.messageClass
            if (messageMatches) {
                handler(this.originStateClass.cast(currentState), this.messageClass.cast(message))
                return true
            }
        }
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Query<*, *, *, *>) return false

        if (originStateClass != other.originStateClass) return false
        if (messageClass != other.messageClass) return false

        return true
    }

    override fun hashCode(): Int {
        var result = originStateClass.hashCode()
        result = 31 * result + messageClass.hashCode()
        return result
    }

    override fun toString(): String {
        return "Query(originStateClass=$originStateClass, messageClass=$messageClass)"
    }
}

data class StateMachineMessage<T>(
    val payload: T,
    val exception: CompletableDeferred<Throwable?> = CompletableDeferred()
)

internal class StateMachine<StateSuperClass : Any, MessageSuperClass : Any>(
    private val transitions: Set<Transition<StateSuperClass, MessageSuperClass, *, *>>,
    private val queries: Set<Query<StateSuperClass, MessageSuperClass, *, *>>,
    initialState: StateSuperClass,
    private val endStateSuperClass: KClass<out StateSuperClass>,
) {
    private var currentState = initialState

    fun handle(message: StateMachineMessage<MessageSuperClass>) {
        try {
            val newState: StateSuperClass? = transitions
                .mapNotNull { it.handle(currentState, message.payload) }
                .singleOrNull()
            if (newState != null) {
//                println("Transitioned from ${currentState::class.simpleName} to ${newState::class.simpleName} on message ${message.payload::class.simpleName}")
                currentState = newState
                message.exception.complete(null)
            } else {
                val query = queries.filter { it.handle(currentState, message.payload) }
                when {
                    query.size == 1 -> {
                        message.exception.complete(null)
                    }
                    query.size > 1 -> {
                        throw UnsupportedOperationException(
                            "Multiple handlers of ${message.payload} in state ${currentState}"
                        )
                    }
                    else -> {
                        throw UnsupportedOperationException(
                            "Unsupported message ${message.payload::class.java} in state ${currentState::class}"
                        ).initCause(null)
                    }
                }
            }
        } catch (e: Exception) {
//            println("Completing state machine message ${message.payload} with exception ${e}")
            message.exception.complete(e)
        }
    }

    fun isInEndState(): Boolean {
        return endStateSuperClass.isInstance(currentState)
    }
}

internal class StateMachineActorPool {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val activeJobs = ConcurrentLinkedQueue<Job>()

    fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        val deferredJobReference = CompletableDeferred<Job>()
        val job: Job = scope.launch(Dispatchers.Default) {
            block()
            val job = withTimeout(100) {
                deferredJobReference.await()
            }
            activeJobs.remove(job)
        }
        activeJobs.add(job)
        deferredJobReference.complete(job)
        return job
    }
}

internal class StateMachineActor<StateSuperClass : Any, MessageSuperClass : Any> private constructor(
    private val stateMachine: StateMachine<StateSuperClass, MessageSuperClass>,
    private val channel: Channel<StateMachineMessage<MessageSuperClass>>,
) {
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
            val messageType =
                msg.payload::class.qualifiedName?.replaceFirst("de.quantummaid.testmaid.internal.", "")
//            println("${Thread.currentThread().name} CHANNEL_MSG_START: $messageType: ${msg.payload}")
            stateMachine.handle(msg)
//            println("CHANNEL_MSG_END: $messageType: ${msg.payload}")
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
}
