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

package de.quantummaid.testmaid.integrations.aws.s3

import de.quantummaid.mapmaid.validatedtypeskotlin.ValidationException
import de.quantummaid.testmaid.localfs.Md5Sum
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class BucketNameTest {

    @Test
    fun testExampleKeysThatShouldPass() {
        val validKeys = listOf(
            "my-bucket", "tld.company.ou.department.service.coder", "letters0123456789-.de"
        )
        validKeys.forEach { BucketName(it) }
    }

    @Test
    fun testInvalidKeyEndCharacter() {
        try {
            BucketName("my-bucket$")
            fail("Expected exception")
        } catch (e: ValidationException) {
            //This is expected
        }
    }

    @Test
    fun testInvalidKeyStartCharacter() {
        try {
            BucketName(".my-bucket")
            fail("Expected exception")
        } catch (e: ValidationException) {
            //This is expected
        }
    }

    @Test
    internal fun testMd5Differs() {
        assertNotEquals(BucketName("goodbye.java").md5Sum(), BucketName("hello.world.kt").md5Sum())
    }

    @Test
    internal fun testMd5Matches() {
        assertEquals(Md5Sum("5d41402abc4b2a76b9719d911017c592"), BucketName("hello").md5Sum())
    }
}
