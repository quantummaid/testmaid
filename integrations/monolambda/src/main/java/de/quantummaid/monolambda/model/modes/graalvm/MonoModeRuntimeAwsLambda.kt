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

package de.quantummaid.monolambda.model.modes.graalvm

import de.quantummaid.monolambda.MonoLambdaDefinition

fun MonoLambdaDefinition.monoModeGraalVm(): MonoModeAwsLambda {
    TODO()
}

class MonoModeAwsLambda() {
    fun enterRuntime(): MonoModeRuntimeAwsLambda {

        TODO()
    }
}

/**
 * Routes the event to a specific component responsible for handling the event. This component also HAS to do error
 * handling, since error handling of an SQS message is different from error handling of an HTTP request from Api
 * Gateway.
 *
 * If there is an error happening when resolving the component, no component could be found or there are multiple
 * components competing for the same event, an exception has to be thrown. This will trigger the default AWS error
 * handling of the specific source AWS product, aka Api Gateway, SQS, etc.
 *
 * Examples for errors:
 * If there is an SQS FIFO with commands for an Actor and an error happens resolving the mono component, an
 * exception will trigger a retry with the queue specific back off strategy/dead letter queue handling. This gives the
 * user time to fix the problem and redeploy the Mono Lambda. The fixed version will then pick up the event and handle
 * it. This results in a degraded performance bug instead of a bug causing data inconsistencies or complete errors.
 */
class MonoModeRuntimeAwsLambda(
        val monoLambdaLoggingFacade: MonoLambdaLoggingFacade,
        val componentRouter: MonoComponentRouter
) {
    fun handleRequest(event: Map<String, Any?>?): MutableMap<String, Any>? {
        val component = monoComponentForEvent(event)
        try {
            return component.handle(event)
        } catch (throwable: Throwable) {
            monoLambdaLoggingFacade.errorUnhandledErrorFromMonoComponent(event, component, throwable)
            throw UncaughtExceptionFromMonoComponentException(component, throwable)
        }
    }

    private fun monoComponentForEvent(event: Map<String, Any?>?): MonoComponent {
        val components = componentRouter.componentFor(event)
        val component = if (components.size == 1) {
            components.single()
        } else if (components.isEmpty()) {
            monoLambdaLoggingFacade.errorNoMonoLambdaComponentFoundForEvent(event)
            throw MonoComponentResolutionException()
        } else {
            monoLambdaLoggingFacade.errorConflictingMonoLambdaComponentsFoundForEvent(event, components)
            throw MonoComponentResolutionException()
        }
        return component
    }
}

class UncaughtExceptionFromMonoComponentException(monoComponent: MonoComponent, cause: Throwable) :
        Exception("Mono component ${monoComponent.description} did not handle the error.", cause)

class MonoComponentResolutionException : Exception("Could not determine a distinct mono component for event handling")

/**
 * This Facade does not have access to a trace id, hence we need to support log facades without a trace id.
 */
interface MonoLambdaLoggingFacade {
    fun errorNoMonoLambdaComponentFoundForEvent(event: Map<String, Any?>?)

    fun errorConflictingMonoLambdaComponentsFoundForEvent(event: Map<String, Any?>?,
                                                          components: Collection<MonoComponent>)

    fun errorUnhandledErrorFromMonoComponent(event: Map<String, Any?>?,
                                             monoComponent: MonoComponent,
                                             throwable: Throwable)
}

interface MonoComponent {
    val description: String
    fun handle(event: Map<String, Any?>?): MutableMap<String, Any>?
}

/**
 * Does an all match search sacrificing performance.
 */
interface MonoComponentRouter {
    fun componentFor(event: Map<String, Any?>?): Collection<MonoComponent>
}
