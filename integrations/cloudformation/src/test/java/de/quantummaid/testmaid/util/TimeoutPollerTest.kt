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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

internal class TimeoutPollerTest {

    @Test
    fun withTimeoutDoesNotTimeoutOnFirstAttemptSuccess() {
        val sut = TimeoutPoller()
        val result = sut.withTimeout(seconds(10), milliseconds(250)) { PollingResult.Done<String, String>("Hi") }
        assertTrue(result is PollingResult.Done)
        result as PollingResult.Done
        assertEquals("Hi", result.result)
    }

    @Test
    fun withTimeoutDoesNotTimeoutOnThirdAttemptSuccess() {
        var attempt = 0

        val sut = TimeoutPoller()
        val result: PollingResult<String, String?> = sut.withTimeout(milliseconds(1000), milliseconds(250)) {
            attempt += 1
            if (attempt == 3) {
                PollingResult.Done("Hi")
            } else {
                PollingResult.NotDoneYet(null)
            }
        }
        assertTrue(result is PollingResult.Done)
        result as PollingResult.Done
        assertEquals("Hi", result.result)
    }

    @Test
    fun withTimeoutTimeoutBeforeFirstAttemptResultsInOnePollExecution() {
        var attempt = 0

        val sut = TimeoutPoller()
        val result: PollingResult<String, String?> = sut.withTimeout(nanoseconds(0), milliseconds(250)) {
            attempt += 1
            if (attempt == 3) {
                PollingResult.Done("Hi")
            } else {
                PollingResult.NotDoneYet(null)
            }
        }
        assertEquals(1, attempt)
        assertTrue(result is PollingResult.NotDoneYet)
    }

    @Test
    fun withTimeoutTimeoutAfterThirdAttempt() {
        var attempt = 0

        val sut = TimeoutPoller()
        val result: PollingResult<String, String?> = sut.withTimeout(milliseconds(700), milliseconds(250)) {
            attempt += 1
            if (attempt == 4) {
                PollingResult.Done("Hi")
            } else {
                PollingResult.NotDoneYet("Not done")
            }
        }
        assertEquals(3, attempt)
        assertTrue(result is PollingResult.NotDoneYet)
        assertEquals("Not done", (result as PollingResult.NotDoneYet).information)
    }
    @Test
    fun withTimeoutTimeoutAfterThirdAttemptWithNullNotDoneInformation() {
        var attempt = 0

        val sut = TimeoutPoller()
        val result: PollingResult<String, String?> = sut.withTimeout(milliseconds(700), milliseconds(250)) {
            attempt += 1
            if (attempt == 4) {
                PollingResult.Done("Hi")
            } else {
                PollingResult.NotDoneYet(null)
            }
        }
        assertEquals(3, attempt)
        assertTrue(result is PollingResult.NotDoneYet)
    }

    @Test
    fun withTimeoutCalculatesIntervalDelay() {
        var attempt = 0

        val sut = TimeoutPoller()
        val result: PollingResult<String, String?> = sut.withTimeout(milliseconds(1000), milliseconds(300)) {
            attempt += 1
            if (attempt == 3) {
                PollingResult.Done("Hi")
            } else {
                runBlocking {
                    delay(milliseconds(200))
                }
                PollingResult.NotDoneYet(null)
            }
        }
        assertTrue(result is PollingResult.Done)
        result as PollingResult.Done
        assertEquals("Hi", result.result)
        assertEquals(3, attempt)
    }
}
