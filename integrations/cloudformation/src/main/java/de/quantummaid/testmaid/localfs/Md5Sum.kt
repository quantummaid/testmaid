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
import de.quantummaid.mapmaid.validatedtypeskotlin.validation.StringValidator.Companion.allOf
import de.quantummaid.mapmaid.validatedtypeskotlin.validation.StringValidator.Companion.length
import de.quantummaid.mapmaid.validatedtypeskotlin.validation.StringValidator.Companion.lowercase
import de.quantummaid.mapmaid.validatedtypeskotlin.validation.StringValidator.Companion.regex
import java.math.BigInteger
import java.security.MessageDigest

class Md5Sum(unsafe: String) : ValidatedString(validator, unsafe) {
    companion object {
        private val validator = allOf(length(32, 32), lowercase(), regex("[0-9abcdef]+".toRegex()))

        fun md5SumOf(vararg parts: Md5Sum): Md5Sum {
            return md5SumOf(parts.joinToString(separator = "") { it.mappingValue() })
        }

        fun md5SumOf(text: String): Md5Sum {
            return md5SumOf(text.toByteArray(Charsets.UTF_8))
        }

        fun md5SumOf(bytes: ByteArray): Md5Sum {
            //Message digest is not ThreadSafe, so keep this here instead of in the companion
            val messageDigest = MessageDigest.getInstance("MD5")
            val md5 = BigInteger(1, messageDigest.digest(bytes)).toString(16).padStart(32, '0')
            return Md5Sum(md5)
        }
    }
}
