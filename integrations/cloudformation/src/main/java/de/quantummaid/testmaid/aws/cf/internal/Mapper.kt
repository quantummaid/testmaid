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

package de.quantummaid.testmaid.aws.cf.internal

import de.quantummaid.testmaid.integrations.aws.cf.plain.api.*
import de.quantummaid.testmaid.util.toIntExactIMeanWhoWantsToRiskAFuckingOverflow
import software.amazon.awssdk.services.cloudformation.model.*
import kotlin.time.Duration

internal class Mapper {
    companion object {
        val UPDATE_FAILURE_STATES = setOf(
            StackStatus.UPDATE_ROLLBACK_COMPLETE,
            StackStatus.UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS,
            StackStatus.UPDATE_ROLLBACK_FAILED,
            StackStatus.UPDATE_ROLLBACK_IN_PROGRESS
        )

        val CREATE_FAILURE_STATES = setOf(
            StackStatus.CREATE_FAILED,
            StackStatus.ROLLBACK_IN_PROGRESS,
            StackStatus.ROLLBACK_COMPLETE,
            StackStatus.ROLLBACK_FAILED
        )

        val stackNotFoundExceptionMessageRegexp = "Stack with id .+ does not exist".toRegex()

        fun mapToCreateStackRequest(stackDefinition: StackDefinition, timeout: Duration): CreateStackRequest {
            val sdkParameters: Collection<Parameter> = toParameters(stackDefinition)
            val tags: Collection<Tag> = toTags(stackDefinition)
            return CreateStackRequest.builder()
                .stackName(stackDefinition.stackName.mappingValue())
                .capabilities(stackDefinition.capabilities)
                .templateBody(stackDefinition.body.mappingValue())
                .timeoutInMinutes(timeout.inWholeMinutes.toIntExactIMeanWhoWantsToRiskAFuckingOverflow())
                .parameters(sdkParameters)
                .tags(tags)
                .build()
        }

        fun mapToUpdateStackRequest(stackDefinition: StackDefinition): UpdateStackRequest {
            val sdkParameters: Collection<Parameter> = toParameters(stackDefinition)
            val tags: Collection<Tag> = toTags(stackDefinition)
            return UpdateStackRequest.builder()
                .stackName(stackDefinition.stackName.mappingValue())
                .capabilities(stackDefinition.capabilities)
                .templateBody(stackDefinition.body.mappingValue())
                .parameters(sdkParameters)
                .tags(tags)
                .build()
        }

        fun mapToCreatedStack(sdkStack: Stack): DetailedStackInformation {
            val outputs = sdkStack.outputs().map {
                val outputName = OutputName(it.outputKey())
                val outputValue = OutputValue(it.outputValue())
                Pair(outputName, outputValue)
            }.toSet()
            val stackName = StackName(sdkStack.stackName())
            val stackId = StackId(sdkStack.stackId())
            return DetailedStackInformation(stackName, stackId, outputs)
        }

        fun isStackNotFoundException(e: CloudFormationException): Boolean {
            return stackNotFoundExceptionMessageRegexp.find(e.message ?: "") != null
        }

        private fun toTags(stackDefinition: StackDefinition) = stackDefinition.tags.tags.map {
            Tag.builder()
                .key(it.first.mappingValue())
                .value(it.second.mappingValue())
                .build()
        }

        private fun toParameters(stackDefinition: StackDefinition) =
            stackDefinition.parameters.parameters.map {
                Parameter.builder()
                    .parameterKey(it.first.mappingValue())
                    .parameterValue(it.second.mappingValue())
                    .build()
            }

        fun mapToCreatedStack(stackSummary: StackSummary): StackReference {
            val stackName = StackName(stackSummary.stackName())
            val stackId = StackId(stackSummary.stackId())
            return StackReference(stackId, stackName)
        }
    }
}
