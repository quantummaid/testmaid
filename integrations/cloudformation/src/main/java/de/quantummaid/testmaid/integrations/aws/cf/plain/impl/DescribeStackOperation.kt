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

package de.quantummaid.testmaid.integrations.aws.cf.plain.impl

import de.quantummaid.testmaid.aws.cf.internal.Mapper
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.StackId
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.StackName
import software.amazon.awssdk.services.cloudformation.CloudFormationClient
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest
import software.amazon.awssdk.services.cloudformation.model.Stack

internal class DescribeStackOperation(private val client: CloudFormationClient) {
    fun describeSingleStack(stackId: StackId): Stack? {
        return describeSingleStack { stackName(stackId.mappingValue()) }
    }

    fun describeSingleStack(name: StackName): Stack? {
        return describeSingleStack { stackName(name.mappingValue()) }
    }

    private fun describeSingleStack(block: DescribeStacksRequest.Builder.() -> Unit): Stack? {
        val request = DescribeStacksRequest.builder().apply(block).build()
        val stacks = try {
            client.describeStacksPaginator(request).stacks().toList()
        } catch (e: CloudFormationException) {
            if (Mapper.isStackNotFoundException(e)) {
                return null
            } else {
                throw e
            }
        }
        return if (stacks.size > 1) {
            val stackInformation = stacks.map {
                "StackId: ${it.stackId()}, StackName: ${it.stackName()}, " +
                        "Status: ${it.stackStatusAsString()}, StatusReason: ${it.stackStatusReason()}"
            }
            val stackName = request.stackName()
            throw UnsupportedOperationException(
                "Describing single stack with name/id '$stackName' resulted in more than one stack: ${stackInformation}"
            )
        } else {
            stacks.single()
        }
    }
}
