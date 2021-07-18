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

package de.quantummaid.testmaid.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.time.Duration

class PollingTimoutException(timeout: Duration, pollingInterval: Duration) : Exception(
    "Polling timed out after ${timeout} while polling with interval ${pollingInterval}"
)

sealed class PollingResult<R, I> {
    data class Done<R, I>(val result: R) : PollingResult<R, I>()
    data class NotDoneYet<R, I>(val information: I) : PollingResult<R, I>()
}

class TimeoutPoller(private val clock: Clock = Clock.System) {

    fun <R, I> withTimeoutOrFail(timeout: Duration, pollingInterval: Duration, block: () -> PollingResult<R, I>): R {
        when (val result = withTimeout(timeout, pollingInterval, block)) {
            is PollingResult.Done -> return result.result
            is PollingResult.NotDoneYet -> throw PollingTimoutException(timeout, pollingInterval)
        }
    }

    fun <R, I> withTimeout(
        timeout: Duration,
        pollingInterval: Duration,
        block: () -> PollingResult<R, I>
    ): PollingResult<R, I> {
        var pollingAttemptStart = clock.now()
        val pollingEnds = pollingAttemptStart + timeout
        var lastNotDoneYetResult: PollingResult.NotDoneYet<R, I>
        do {
            val pollingResult = block()
            val pollingAttemptEnd = clock.now()
            when (pollingResult) {
                is PollingResult.Done -> return pollingResult
                is PollingResult.NotDoneYet -> {
                    lastNotDoneYetResult = pollingResult
                    val nextPollingAttemptAt = pollingAttemptStart + pollingInterval
                    if (pollingAttemptEnd < nextPollingAttemptAt) {
                        runBlocking {
                            val delayDuration = nextPollingAttemptAt - pollingAttemptEnd
                            delay(delayDuration)
                        }
                    }
                    pollingAttemptStart = clock.now()
                }
            }
        } while (pollingAttemptStart < pollingEnds)
        return lastNotDoneYetResult
    }
}
