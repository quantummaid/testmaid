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

package de.quantummaid.monolambda.model.logging

import de.quantummaid.monolambda.*
import de.quantummaid.monolambda.cf.CloudFormationTemplatePart
import de.quantummaid.monolambda.cf.parts.logs.LogGroupRetentionInDays
import de.quantummaid.monolambda.cf.parts.logs.MonoLambdaLogging
import de.quantummaid.monolambda.model.cfg.EnvVariableName

class MonoLambdaLoggingDefinition() : MonoLambdaComponentDefinition {
    companion object {
        private val logRetentionInDaysParameter = ConfigurationParameterDefinition(
                EnvVariableName("LOG_GROUP_RETENTION_IN_DAYS"), ::LogGroupRetentionInDays,
                "Specifies the number of days after which a log statement should be " +
                        "deleted from the cloud watch log group."
        )
    }

    override fun configurationDefinition(): ConfigurationDefinition {
        return ConfigurationDefinition("MonoLambdaLogging", listOf(logRetentionInDaysParameter))
    }

    override fun conflicts(other: MonoLambdaComponentDefinition): MonoLambdaComponentDefinition.Companion.Conflict? {
        return if (other::class == MonoLambdaLoggingDefinition::class) {
            MonoLambdaComponentDefinition.Companion.Conflict(
                    "MonoLambdaLoggingDefinition can only be configured once"
            )
        } else {
            null
        }
    }

    fun instantiate(configuration: Configuration): MonoLambdaComponent {
        val logGroupRetentionInDays = configuration.valueOf(logRetentionInDaysParameter)

        return object : MonoLambdaComponent {
            override fun cloudformationParts(): Collection<CloudFormationTemplatePart> {
                return listOf(MonoLambdaLogging(logGroupRetentionInDays))
            }
        }
    }
}
