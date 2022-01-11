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

import kotlin.reflect.KClass
import kotlin.reflect.cast

data class Query<StateSuperClass : Any, MessageSuperClass : Any, OriginState : StateSuperClass, Message : MessageSuperClass>(
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
}