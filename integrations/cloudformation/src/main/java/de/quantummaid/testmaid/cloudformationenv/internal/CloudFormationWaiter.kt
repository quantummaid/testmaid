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

package de.quantummaid.testmaid.cloudformationenv.internal

import software.amazon.awssdk.services.cloudformation.CloudFormationClient
import software.amazon.awssdk.services.cloudformation.model.*
import software.amazon.awssdk.services.cloudformation.model.Stack
import java.time.Instant
import java.util.*
import java.util.function.Predicate

class StackWaitWouldNeverCompleteException(private val msg: String) : RuntimeException(msg) {
    fun probablyExplainedBy(events: List<StackEvent>): StackUpdateFailedException {
        val betterMsg = "$msg, probably explained by: ${
            events.joinToString(
                prefix = "[\n- ",
                separator = "\n- ",
                postfix = "\n]",
                transform = {
                    "${it.timestamp()} ${it.resourceStatusAsString()} for ${it.resourceType()}@${it.logicalResourceId()} because:${it.resourceStatusReason()}"
                }
            )
        }"
        return StackUpdateFailedException(betterMsg)
    }
}

class StackUpdateFailedException(msg: String) : RuntimeException(msg)

/**
 * Credits for the various error states:
 * https://cloudacademy.com/course/advanced-aws-cloudformation/data-flow/ @ 3:56
 */
object CloudFormationWaiter {
    private const val MAX_NUMBER_OF_TRIES = 60
    private const val SLEEP_TIME_IN_MILLISECONDS = 5000

    private val UPDATE_FAILURE_STATES = setOf(
        StackStatus.UPDATE_ROLLBACK_COMPLETE,
        StackStatus.UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS,
        StackStatus.UPDATE_ROLLBACK_FAILED,
        StackStatus.UPDATE_ROLLBACK_IN_PROGRESS
    )

    private val CREATE_FAILURE_STATES = setOf(
        StackStatus.CREATE_FAILED,
        StackStatus.ROLLBACK_IN_PROGRESS,
        StackStatus.ROLLBACK_COMPLETE,
        StackStatus.ROLLBACK_FAILED
    )

    private val RESOURCE_FAILURE_STATES_WITH_DIAGNOSTIC_INFO = setOf(
        ResourceStatus.CREATE_FAILED,
        ResourceStatus.UPDATE_FAILED
    )

    fun updateStackSync(
        client: CloudFormationClient,
        request: UpdateStackRequest
    ) {
        val stackName = request.stackName()
        val stackLastUpdatedTime = lastStackEventFor(client, stackName)
        try {
            client.updateStack(request)
            waitFor(
                client,
                stackName,
                StackStatus.UPDATE_COMPLETE,
                UPDATE_FAILURE_STATES
            )
        } catch (e: StackWaitWouldNeverCompleteException) {
            val events: List<StackEvent> = client.describeStackEvents { it.stackName(stackName) }
                .stackEvents()
                .filter { it.timestamp().isAfter(stackLastUpdatedTime) }
                .filter { it.resourceStatus() in RESOURCE_FAILURE_STATES_WITH_DIAGNOSTIC_INFO }
            throw e.probablyExplainedBy(events)
        }
    }

    private fun lastStackEventFor(
        client: CloudFormationClient,
        stackNameOrId: String
    ): Instant = client
        .describeStackEvents { it.stackName(stackNameOrId) }
        .stackEvents()
        .stream()
        .findFirst()
        .map(StackEvent::timestamp)
        .orElse(Instant.now())

    fun createStackSync(
        client: CloudFormationClient,
        request: CreateStackRequest
    ) {
        client.createStack(request)
        waitFor(
            client,
            request.stackName(),
            StackStatus.CREATE_COMPLETE,
            CREATE_FAILURE_STATES
        )
    }

    fun deleteStackSync(
        client: CloudFormationClient,
        request: DeleteStackRequest
    ) {
        // The stack will become 'invisible' to describeStacks if we describe by stack name.
        // so we will be using the stack id instead, so that we may look for the DELETE_COMPLETE status.
        val stackName = request.stackName()
        val stackId = stackIdForStackName(client, stackName)
        client.deleteStack(request)
        waitFor(
            client,
            stackId,
            StackStatus.DELETE_COMPLETE,
            setOf(StackStatus.DELETE_FAILED)
        )
    }

    private fun stackIdForStackName(
        cloudFormationClient: CloudFormationClient,
        stackName: String
    ): String = cloudFormationClient
        .describeStacks { it.stackName(stackName) }
        .stacks()
        .stream()
        .findFirst()
        .map(Stack::stackId)
        .orElse(stackName)

    private fun waitFor(
        client: CloudFormationClient,
        stackNameOrId: String,
        successState: StackStatus,
        failureStates: Set<StackStatus>
    ) {
        waitFor(
            client,
            stackNameOrId,
        ) { stack: Optional<Stack> ->
            stack
                .map(Stack::stackStatus)
                .map { stackStatus: Any ->
                    if (failureStates.contains(stackStatus)) {
//                        log.warn {
//                            "Waiting for stack '$stackNameOrId' to reach '$successState' failed (current:'$stackStatus')"
//                        }
                        throw StackWaitWouldNeverCompleteException("stack '$stackNameOrId' became '$stackStatus', would never reach the expected '$successState'")
                    } else {
                        val successStateHasBeenReached = successState == stackStatus
                        if (!successStateHasBeenReached) {
//                            log.info { "Waiting for stack '$stackNameOrId' to reach '$successState' (current:'$stackStatus')..." }
                            false
                        } else {
//                            log.info { "Done waiting for stack '$stackNameOrId' to reach '$successState'" }
                            true
                        }
                    }
                }
                .orElseGet {
//                    log.info { "Waiting for stack '$stackNameOrId' to change state..." }
                    false
                }
        }
    }

    private fun waitFor(
        cloudFormationClient: CloudFormationClient,
        stackIdentifier: String,
        condition: Predicate<Optional<Stack>>
    ) {
//        pollWithTimeout(
//            MAX_NUMBER_OF_TRIES,
//            SLEEP_TIME_IN_MILLISECONDS
//        ) { conditionReached(stackIdentifier, cloudFormationClient, condition) }
    }

    private fun conditionReached(
        stackIdentifier: String,
        cloudFormationClient: CloudFormationClient,
        predicate: Predicate<Optional<Stack>>
    ): Boolean {
        val describeStacksResponse = cloudFormationClient.describeStacks { it.stackName(stackIdentifier) }
        return predicate.test(describeStacksResponse.stacks().stream().findFirst())
    }
}
