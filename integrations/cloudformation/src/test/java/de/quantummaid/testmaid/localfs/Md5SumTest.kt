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

package de.quantummaid.testmaid.localfs

import de.quantummaid.testmaid.localfs.Md5Sum.Companion.md5SumOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class Md5SumTest {

    @Test
    fun testSameMd5() {
        val sumOfHello1 = md5SumOf("Hello")
        val sumOfHello2 = md5SumOf("Hello")
        assertEquals(sumOfHello1, sumOfHello2)
    }

    @Test
    fun testDifferentMd5() {
        val sumOfHello = md5SumOf("Hello")
        val sumOfWorld = md5SumOf("World")
        assertNotEquals(sumOfHello, sumOfWorld)
    }

    @Test
    fun testSameConcatMd5() {
        val sumOfHelloWorld1 = md5SumOf(md5SumOf("Hello"), md5SumOf("World"), md5SumOf("!"))
        val sumOfHelloWorld2 = md5SumOf(md5SumOf("Hello"), md5SumOf("World"), md5SumOf("!"))
        assertEquals(sumOfHelloWorld1, sumOfHelloWorld2)
    }

    @Test
    fun testDifferentConcatMd5() {
        val sumOfHelloSpace = md5SumOf(md5SumOf("Hello"), md5SumOf(" "))
        val sumOfWorldBang = md5SumOf(md5SumOf("World"), md5SumOf("!"))
        assertNotEquals(sumOfHelloSpace, sumOfWorldBang)
    }

    @Test
    fun testMd5SumCompatibleWithOsxMd5() {
        /*
            $ echo -n 'hello' | md5sum
            5d41402abc4b2a76b9719d911017c592
         */
        val actual = md5SumOf("hello")
        assertEquals("5d41402abc4b2a76b9719d911017c592", actual.mappingValue())
    }

    @Test
    fun testMd5SumCanBeCreatedFromMappingValues() {
        assertEquals(Md5Sum("5d41402abc4b2a76b9719d911017c592"), Md5Sum("5d41402abc4b2a76b9719d911017c592"))
        assertNotEquals(Md5Sum("5d41402abc4b2a76b9719d911017c592"), Md5Sum("77f088caf896bc2be55af5cdf8e860ae"))
    }
}
