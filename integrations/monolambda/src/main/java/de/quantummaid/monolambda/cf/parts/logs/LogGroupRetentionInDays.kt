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

package de.quantummaid.monolambda.cf.parts.logs

import de.quantummaid.mapmaid.validatedtypeskotlin.types.ValidatedInt
import de.quantummaid.mapmaid.validatedtypeskotlin.validation.IntValidator
import de.quantummaid.mapmaid.validatedtypeskotlin.validation.ValidationHelper.validate

/**
 * https://docs.aws.amazon.com/de_de/AmazonCloudWatchLogs/latest/APIReference/API_LogGroup.html.
 */
class LogGroupRetentionInDays(unsafe: String) : ValidatedInt(validator, unsafe) {
    companion object {
        private val validDays = setOf(
                1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365, 400, 545, 731, 1827, 3653
        )
        private val validator = IntValidator { validate(it, validDays.contains(it)) { "must be one of ${validDays}" } }
    }
}