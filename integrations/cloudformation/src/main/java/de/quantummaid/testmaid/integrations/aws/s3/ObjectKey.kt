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

import de.quantummaid.mapmaid.validatedtypeskotlin.types.ValidatedString
import de.quantummaid.mapmaid.validatedtypeskotlin.validation.StringValidator.Companion.allOf
import de.quantummaid.mapmaid.validatedtypeskotlin.validation.StringValidator.Companion.length
import de.quantummaid.mapmaid.validatedtypeskotlin.validation.StringValidator.Companion.regex
import de.quantummaid.testmaid.localfs.Md5Sum


/**
 * https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html
 *
 * Safe characters
 * The following character sets are generally safe for use in key names. Alphanumeric characters
 *   0-9
 *   a-z
 *   A-Z
 * Special characters
 *   Forward slash (/)
 *   Exclamation point (!)
 *   Hyphen (-)
 *   Underscore (_)
 *   Period (.)
 *   Asterisk (*)
 *   Single quote (')
 *   Open parenthesis (()
 *   Close parenthesis ())
 * The following are examples of valid object key names:
 *   4my-organization
 *   my.great_photos-2014/jan/myvacation.jpg
 *   videos/2014/birthday/video1.wmv
 *
 */
class ObjectKey(unsafe: String) : ValidatedString(validator, unsafe) {
    companion object {
        private val validator = allOf(length(1, 120), regex("[a-zA-Z0-9/!\\-_.*'()]+".toRegex()))
    }

    fun md5Sum(): Md5Sum {
        return Md5Sum.md5SumOf(mappingValue())
    }
}
