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

import de.quantummaid.mapmaid.validatedtypeskotlin.types.ValidatedString
import de.quantummaid.mapmaid.validatedtypeskotlin.validation.StringValidator
import java.nio.charset.Charset
import kotlin.io.path.Path
import kotlin.io.path.readBytes
import kotlin.io.path.readText

/**
 * May need some more validation here, but security should not be such a big deal here, since the code will run in test
 * environments only.
 */
class LocalFile(unsafe: String) : ValidatedString(localPathValidator, unsafe) {
    companion object {
        private val localPathValidator = StringValidator.allOf(StringValidator.length(1, 256))
    }

    fun md5Sum(): Md5Sum {
        val path = Path(mappingValue())
        val bytes = path.readBytes()
        return Md5Sum.md5SumOf(bytes)
    }

    fun <T> mapContentTo(mapper: (String) -> T, charset: Charset = Charsets.UTF_8): T {
        val path = Path(mappingValue())
        val text = path.readText(charset)
        return mapper(text)
    }

    fun stringContent(charset: Charset = Charsets.UTF_8): String {
        val path = Path(mappingValue())
        val text = path.readText(charset)
        return text
    }
}
