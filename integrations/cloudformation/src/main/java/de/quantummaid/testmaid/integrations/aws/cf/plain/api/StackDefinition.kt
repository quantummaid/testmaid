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

package de.quantummaid.testmaid.integrations.aws.cf.plain.api

import de.quantummaid.testmaid.integrations.aws.Tags
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.Parameters.Companion.parameters
import de.quantummaid.testmaid.localfs.Md5Sum
import de.quantummaid.testmaid.localfs.Md5Sum.Companion.md5SumOf
import software.amazon.awssdk.services.cloudformation.model.Capability
import software.amazon.awssdk.services.cloudformation.model.Capability.*

data class StackDefinition(
    val stackName: StackName,
    val body: Body,
    val parameters: Parameters = parameters(),
    val tags: Tags = Tags(listOf()),
    val capabilities: Set<Capability> = allCapabilities,
) {
    companion object {
        private val allCapabilities = setOf(CAPABILITY_IAM, CAPABILITY_NAMED_IAM, CAPABILITY_AUTO_EXPAND)
    }

    fun md5Sum(): Md5Sum {
        val capabilitiesMd5Sum = md5SumOf(capabilities.joinToString("") { it.name })

        return md5SumOf(
            stackName.md5Sum(), body.md5Sum(), parameters.md5Sum(), tags.md5Sum(), capabilitiesMd5Sum
        )
    }

    override fun toString(): String {
        return "StackDefinition(stackName=$stackName, body='...', parameters=$parameters, tags=$tags, capabilities=$capabilities)"
    }
}
