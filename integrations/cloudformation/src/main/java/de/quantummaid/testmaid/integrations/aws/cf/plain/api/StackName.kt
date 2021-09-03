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

import de.quantummaid.mapmaid.validatedtypeskotlin.types.ValidatedString
import de.quantummaid.mapmaid.validatedtypeskotlin.validation.StringValidator.Companion.allOf
import de.quantummaid.mapmaid.validatedtypeskotlin.validation.StringValidator.Companion.length
import de.quantummaid.mapmaid.validatedtypeskotlin.validation.StringValidator.Companion.regex
import de.quantummaid.testmaid.localfs.Md5Sum
import de.quantummaid.testmaid.localfs.Md5Sum.Companion.md5SumOf
import java.util.*


/**
 * A stack name can contain only alphanumeric characters (case sensitive) and hyphens.
 * It must start with an alphabetic character and cannot be longer than 128 characters.
 */
class StackName(unsafe: String) : ValidatedString(validator, unsafe) {
    companion object {
        private val regex = "[^a-zA-Z0-9-]".toRegex()

        fun cleanupForUsageAsStackName(string: String): String {
            return string.replace(regex, "")
        }

        private val validator = allOf(length(1, 128), regex("[a-zA-Z][a-zA-Z0-9-]*".toRegex()))
    }

    fun md5Sum(): Md5Sum {
        return md5SumOf(mappingValue())
    }

    fun startsWith(stackPrefix: StackPrefix): Boolean {
        return this.safeValue.startsWith(stackPrefix.mappingValue())
    }
}

class StackPrefix(unsafe: String) : ValidatedString(validator, unsafe) {
    companion object {
        private val validator = allOf(length(1, 128), regex("[a-zA-Z][a-zA-Z0-9-]*".toRegex()))
    }

    fun generateUniqueName(): StackName {
        val uuid = UUID.randomUUID().toString()
        val unique = uuid.subSequence(16..24)
        return StackName("${safeValue}${unique}")
    }
}

class StackNameBuilder(val project: StackPrefix, val runtimeId: StackPrefix) {
    fun uniqueForTestCase(testCase: String): StackName {
        return prefixForTestCase(testCase).generateUniqueName()
    }

    fun prefixForTestCase(testCase: String): StackPrefix {
        val cleanedName = StackName.cleanupForUsageAsStackName(testCase)
        return StackPrefix("${project.mappingValue()}-${runtimeId.mappingValue()}-${cleanedName}")
    }
}