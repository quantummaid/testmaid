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

import de.quantummaid.monolambda.model.MonoLambdaName
import de.quantummaid.monolambda.model.cfg.EnvVariableName
import de.quantummaid.monolambda.model.cfg.EnvVariables

/**
 * This class is intended to do two things and report their results in an aggregated and also NICE WAY:
 * 1. Gather missing configuration input
 * 2. Gather invalid configuration input
 *
 * Imagine you have now clue what is going on, trying to get thing running you've never seen before and it throws
 * a configuration exception without a message - your feel left alone. This class should eliminate this feeling
 * and report everything that has to be done in order to fix all config errors so we do not send the user on a round
 * trip of hell
 *
 * TODO: Implement what has been documented here :)
 */
class MonoLambdaConfigurationFactory(private val configurationDefinitions: List<ConfigurationDefinition>) {
    fun loadFromEnvironment(envVariables: EnvVariables): Configuration {
        val configurationParameter = configurationDefinitions.flatMap { definition ->
            definition.parameters.map { parameter ->
                val envValue = if (parameter.defaultValue != null) {
                    envVariables.byNameOrDefault(parameter.name.mappingValue(), parameter.defaultValue)
                } else {
                    envVariables.requiredByName(parameter.name.mappingValue())
                }
                val validatedValue = parameter.targetTypeFactory(envValue)
                ConfigurationParameter(parameter.name, validatedValue)

            }
        }
        return Configuration(MonoLambdaName(""), configurationParameter)
    }
}

data class ConfigurationParameterDefinition<TargetType : Any>(
        val name: EnvVariableName,
        val targetTypeFactory: (String) -> TargetType,
        val description: String,
        val defaultValue: String? = null
)

data class ConfigurationParameter<TargetType : Any>(
        val name: EnvVariableName,
        val value: TargetType
)

class ConfigurationDefinition(
        val componentDescription: String,
        val parameters: List<ConfigurationParameterDefinition<*>>
) {
    companion object {
        fun noConfigurationRequired(componentDescription: String): ConfigurationDefinition {
            return ConfigurationDefinition(componentDescription, listOf())
        }
    }
}

class Configuration(
        val monoLambdaName: MonoLambdaName,
        private val values: List<ConfigurationParameter<Any>>
) {

    @Suppress("UNCHECKED_CAST")
    fun <TargetType : Any> valueOf(definition: ConfigurationParameterDefinition<TargetType>): TargetType {
        return this.values.single { it.name == definition.name }.value as TargetType
    }


}
