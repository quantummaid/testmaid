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
import org.junit.jupiter.api.Test
import java.util.*

internal class LocalFileTest {

    @Test
    fun md5Sum() {
        val directory = LocalDirectory.targetDirectoryOfClass(LocalFileTest::class.java)
        val content = "Hello World!"
        val localFile = directory.createFile(UUID.randomUUID().toString(), content.toByteArray(Charsets.UTF_8))

        assertEquals(md5SumOf(content), localFile.md5Sum())
    }

    data class TestTarget(val content: String)

    @Test
    fun mapContentTo() {
        val directory = LocalDirectory.targetDirectoryOfClass(LocalFileTest::class.java)
        val content = "Hello World!"
        val localFile = directory.createFile(UUID.randomUUID().toString(), content.toByteArray(Charsets.UTF_8))

        var stubTarget = "empty"
        val actual = localFile.mapContentTo(::TestTarget)
        assertEquals(TestTarget("Hello World!"), actual)
    }

    @Test
    fun stringContent() {
        val directory = LocalDirectory.targetDirectoryOfClass(LocalFileTest::class.java)
        val content = "Hello World!"
        val localFile = directory.createFile(UUID.randomUUID().toString(), content.toByteArray(Charsets.UTF_8))
        assertEquals(content, localFile.stringContent(Charsets.UTF_8))
    }

    @Test
    fun stringContentDefaultCharset() {
        val directory = LocalDirectory.targetDirectoryOfClass(LocalFileTest::class.java)
        val content = "Hello World!"
        val localFile = directory.createFile(UUID.randomUUID().toString(), content.toByteArray(Charsets.UTF_8))
        assertEquals(content, localFile.stringContent())
    }
}
