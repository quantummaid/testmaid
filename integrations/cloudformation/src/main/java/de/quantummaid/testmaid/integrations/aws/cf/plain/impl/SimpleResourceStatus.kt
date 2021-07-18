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

import software.amazon.awssdk.services.cloudformation.model.ResourceStatus
import software.amazon.awssdk.services.cloudformation.model.StackResource

class ResourceIntel(
    val logicalId: String,
    val type: String,
    val status: String,
    val reason: String?,
    val physicalId: String?
) {
    override fun toString(): String {
        return "$type[$logicalId]_[$status]:'$reason'(${physicalId ?: "No physical id"})"
    }
}

sealed class SimpleResourceStatus(val intel: ResourceIntel) {
    override fun toString(): String {
        return intel.toString()
    }

    companion object {
        fun fromSdkResource(stackResource: StackResource): SimpleResourceStatus {
            val intel = ResourceIntel(
                stackResource.logicalResourceId(),
                stackResource.resourceType(),
                stackResource.resourceStatusAsString(),
                stackResource.resourceStatusReason(),
                stackResource.physicalResourceId(),
            )
            return when (val resourceStatus: ResourceStatus = stackResource.resourceStatus()) {
                ResourceStatus.CREATE_IN_PROGRESS -> InProgress(intel)
                ResourceStatus.CREATE_FAILED -> Failed(intel)
                ResourceStatus.CREATE_COMPLETE -> Success(intel)
                ResourceStatus.DELETE_IN_PROGRESS -> InProgress(intel)
                ResourceStatus.DELETE_FAILED -> Failed(intel)
                ResourceStatus.DELETE_COMPLETE -> Success(intel)
                ResourceStatus.DELETE_SKIPPED -> Failed(intel)
                ResourceStatus.UPDATE_IN_PROGRESS -> InProgress(intel)
                ResourceStatus.UPDATE_FAILED -> Failed(intel)
                ResourceStatus.UPDATE_COMPLETE -> Success(intel)
                ResourceStatus.IMPORT_FAILED -> Failed(intel)
                ResourceStatus.IMPORT_COMPLETE -> Success(intel)
                ResourceStatus.IMPORT_IN_PROGRESS -> InProgress(intel)
                ResourceStatus.IMPORT_ROLLBACK_IN_PROGRESS -> InProgress(intel)
                ResourceStatus.IMPORT_ROLLBACK_FAILED -> Failed(intel)
                ResourceStatus.IMPORT_ROLLBACK_COMPLETE -> Failed(intel)
                ResourceStatus.UNKNOWN_TO_SDK_VERSION -> throw UnsupportedOperationException("Unknown resource state ${resourceStatus}")
            }
        }
    }

    class InProgress(intel: ResourceIntel) : SimpleResourceStatus(intel)
    class Success(intel: ResourceIntel) : SimpleResourceStatus(intel)
    class Failed(intel: ResourceIntel) : SimpleResourceStatus(intel)
}
