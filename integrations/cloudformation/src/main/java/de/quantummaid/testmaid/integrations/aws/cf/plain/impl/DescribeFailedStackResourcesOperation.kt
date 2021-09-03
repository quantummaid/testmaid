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

import de.quantummaid.testmaid.integrations.aws.cf.plain.api.StackId
import software.amazon.awssdk.services.cloudformation.CloudFormationClient
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus

internal class DescribeFailedStackResourcesOperation(private val client: CloudFormationClient) {
    companion object {
        private val failedResourceStates = setOf(
                ResourceStatus.CREATE_FAILED,
                ResourceStatus.DELETE_FAILED,
                ResourceStatus.IMPORT_FAILED,
                ResourceStatus.IMPORT_ROLLBACK_FAILED,
                ResourceStatus.UPDATE_FAILED,
        )
    }

    fun describeFailedResources(stackId: StackId): List<String> {
        val listStackResourcesPaginator = client.listStackResourcesPaginator { it.stackName(stackId.mappingValue()) }
        val stackResourceSummaries = listStackResourcesPaginator.stackResourceSummaries()
        val failed = stackResourceSummaries.filter { failedResourceStates.contains(it.resourceStatus()) }
        return failed.map {
            "${it.resourceType()}(${it.logicalResourceId()}): ${it.resourceStatus()}(${it.resourceStatusReason()})"
        }
    }
}
