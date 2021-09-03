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

import de.quantummaid.reflectmaid.actors.ActorPool
import de.quantummaid.testmaid.internal.statemachine.StateMachineActor.Companion.launch
import kotlin.reflect.KClass

internal class StateMachineBuilder<StateSuperClass : Any, MessageSuperClass : Any>(private val name: String) {
    companion object {
        val actorPool = ActorPool(ActorPool.fixedThreadPoolDispatcher(10, "TestMaidActorPool"))

        fun <StateSuperClass : Any, MessageSuperClass : Any> aStateMachineUsing(
            name: String
        ): StateMachineBuilder<StateSuperClass, MessageSuperClass> {
            return StateMachineBuilder(name)
        }
    }

    private val transitions = mutableSetOf<Transition<StateSuperClass, MessageSuperClass, *, *, *>>()
    private val queries = mutableSetOf<Query<StateSuperClass, MessageSuperClass, *, *>>()
    private var initialState: StateSuperClass? = null
    private var endStateSuperClass: KClass<out StateSuperClass>? = null

    fun withInitialState(initial: StateSuperClass): StateMachineBuilder<StateSuperClass, MessageSuperClass> {
        this.initialState = initial
        return this
    }

    inline fun <reified EndStateSuperClass : StateSuperClass> withEndStateSuperClass(): StateMachineBuilder<StateSuperClass, MessageSuperClass> {
        this.endStateSuperClass = EndStateSuperClass::class
        return this
    }

    inline fun <reified OriginState : StateSuperClass, reified Message : MessageSuperClass> withQuery(
        noinline block: OriginState.(Message) -> Unit
    ): StateMachineBuilder<StateSuperClass, MessageSuperClass> {
        queries.add(Query(OriginState::class, Message::class, block))
        return this
    }

    inline fun <
            reified OriginState : StateSuperClass,
            reified Message : MessageSuperClass,
            reified TargetState : StateSuperClass
            > withTransition(
        noinline block: OriginState.(Message) -> TargetState
    ): StateMachineBuilder<StateSuperClass, MessageSuperClass> {
        transitions.add(
            Transition(OriginState::class, TargetState::class, Message::class, block)
        )
        return this
    }

    fun build(): StateMachineActor<StateSuperClass, MessageSuperClass> {
        val stateMachine: StateMachine<StateSuperClass, MessageSuperClass> = StateMachine(
            transitions, queries, initialState!!, endStateSuperClass!!
        )
        return launch(name, actorPool, stateMachine)
    }
}
