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
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.StackPrefix
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.StackReference
import software.amazon.awssdk.services.cloudformation.CloudFormationClient

internal class ListStacksWithPrefixOperation(private val client: CloudFormationClient) {
    fun listWithPrefix(stackPrefix: StackPrefix): List<StackReference> {
        val listStacksPaginator =
            client.listStacksPaginator {
                val notDeletedSdkStates = SimpleResourceStatus.notDeletedSdkStates()
                it.stackStatusFilters(notDeletedSdkStates)
            }
        val stackSummaries = listStacksPaginator.stackSummaries()
        val withPrefix = stackSummaries.map {
            Mapper.mapToCreatedStack(it)
        }.filter {
            it.stackName.startsWith(stackPrefix)
        }
        return withPrefix
    }

}
