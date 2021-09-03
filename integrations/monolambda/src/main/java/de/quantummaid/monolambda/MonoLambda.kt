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

package de.quantummaid.monolambda

import de.quantummaid.monolambda.cf.CloudFormationTemplatePart
import de.quantummaid.monolambda.cf.MonoLambdaTemplate
import de.quantummaid.monolambda.cf.parts.lambda.LambdaFunctionMemorySize
import de.quantummaid.monolambda.cf.parts.lambda.LambdaFunctionTimeout
import de.quantummaid.monolambda.cf.parts.logs.LogGroupRetentionInDays
import de.quantummaid.monolambda.cf.parts.logs.MonoLambdaLogging
import de.quantummaid.monolambda.model.MonoLambdaName
import de.quantummaid.monolambda.model.cfg.EnvVariableName
import kotlin.reflect.KClass


class MonoLambdaDefinition(
        val monoLambdaName: MonoLambdaName,
        val memorySize: LambdaFunctionMemorySize,
        val timeout: LambdaFunctionTimeout,
        val components: Collection<MonoLambdaComponentDefinition>
) {

    fun configurationFactory(): MonoLambdaConfigurationFactory {
        val configurationDefinitions = components.map { it.configurationDefinition() }
        return MonoLambdaConfigurationFactory(configurationDefinitions)
    }

}

class MonoLambdaComponentDefinitions(val componentsByType: MutableMap<KClass<out MonoLambdaComponentDefinition>, MonoLambdaComponentDefinition>) {
    companion object {
        fun componentDefinitions(components: Collection<MonoLambdaComponentDefinition>): MonoLambdaComponentDefinitions {
            val componentsByType: MutableMap<KClass<out MonoLambdaComponentDefinition>, MonoLambdaComponentDefinition> = components.associateBy {
                it::class
            }.toMutableMap()
            return MonoLambdaComponentDefinitions(componentsByType)
        }
    }
}

interface MonoLambdaComponentDefinition {
    fun conflicts(other: MonoLambdaComponentDefinition): Conflict? {
        return null
    }

    fun configurationDefinition(): ConfigurationDefinition

    companion object {
        data class Conflict(val reason: String)
    }
}



interface MonoLambdaComponent {
    fun cloudformationParts(): Collection<CloudFormationTemplatePart>
}

/* Here follows some test code that probably should not be available in production code */
interface MonoLambdaSemiRemoteComponent {
    fun cloudformationParts(): Collection<CloudFormationTemplatePart>
}

interface MonoLambdaInMemoryComponent {
    fun cloudformationParts(): Collection<CloudFormationTemplatePart>
}

class MonoLambda(
        val monoLambdaName: MonoLambdaName,
        val logRetentionInDays: LogGroupRetentionInDays = LogGroupRetentionInDays("7"),
        val memorySize: LambdaFunctionMemorySize = LambdaFunctionMemorySize("256"),
        val timeout: LambdaFunctionTimeout = LambdaFunctionTimeout("20"),
        val configuredParts: Collection<MonoLambdaComponent>
) {
    fun cloudFormationTemplate() {
        val allParts: MutableMap<KClass<out MonoLambdaComponent>, MonoLambdaComponent> = configuredParts.associateBy {
            it::class
        }.toMutableMap()


        /*
        if (actorTables.isNotEmpty()) {
            parts.add(ActorFiFo(monoLambdaName, timeout.messageRetentionPeriod(), timeout.messageVisibilityTimeout()))
            parts.addAll(actorTables)
        }
        if (requiresStatelessActionQueue) {
            parts.add(StatelessActionQueue(monoLambdaName, timeout.messageRetentionPeriod(), timeout.messageVisibilityTimeout()))
        }
         */
        MonoLambdaTemplate(
                memorySize = LambdaFunctionMemorySize("256"),
                timeout = LambdaFunctionTimeout("10"), parts = configuredParts.flatMap { it -> it.cloudformationParts() }
        ).render()
    }
}
