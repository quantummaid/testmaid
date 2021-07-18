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

import de.quantummaid.mapmaid.validatedtypeskotlin.ValidationException
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.Parameters.Companion.parameters
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ParametersTest {

    @Test
    fun md5SumEquals() {
        assertEquals(
            parameters(
                "hello" to "world",
                "name" to "Kevin",
                "greetings" to "all",
            ).md5Sum(),
            parameters(
                "hello" to "world",
                "name" to "Kevin",
                "greetings" to "all",
            ).md5Sum()
        )
    }

    @Test
    fun md5SumDiffersInValue() {
        assertNotEquals(
            parameters(
                "hello" to "world",
                "name" to "Kevin",
                "greetings" to "Mitnick",
            ).md5Sum(),
            parameters(
                "hello" to "world",
                "name" to "Kevin",
                "greetings" to "Gates",
            ).md5Sum()
        )
    }

    @Test
    fun md5SumDiffersInKey() {
        assertNotEquals(
            parameters(
                "hello" to "world",
                "person" to "Kevin",
                "greetings" to "all",
            ).md5Sum(),
            parameters(
                "hello" to "world",
                "name" to "Kevin",
                "greetings" to "all",
            ).md5Sum()
        )
    }

    @Test
    fun md5SumDiffersInCompleteParameterPair() {
        assertNotEquals(
            parameters(
                "person" to "Kevin",
                "greetings" to "all",
            ).md5Sum(),
            parameters(
                "hello" to "world",
                "name" to "Kevin",
                "greetings" to "all",
            ).md5Sum()
        )
    }

    @Test
    fun passesForEmptyParameters() {
         Parameters(listOf())
         parameters()
    }

    @Test
    fun passesFor200() {
        assertNotNull(Parameters((1..200).map { ParameterKey("key $it") to ParameterValue("Value $it") }))
    }

    @Test
    fun failsFor201() {
        val validationException = assertThrows<ValidationException> {
            assertNotNull(Parameters((1..201).map {
                ParameterKey("key $it") to ParameterValue("Value $it")
            }))
        }
        assertEquals("must contain between 1 and 200 parameters", validationException.message)
    }
}
