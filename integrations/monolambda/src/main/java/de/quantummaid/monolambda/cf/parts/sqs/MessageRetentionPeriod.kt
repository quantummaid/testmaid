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

package de.quantummaid.monolambda.cf.parts.sqs

import de.quantummaid.mapmaid.validatedtypeskotlin.types.ValidatedInt
import de.quantummaid.mapmaid.validatedtypeskotlin.validation.IntValidator

/**
 * The number of seconds that Amazon SQS retains a message. You can specify an integer value from 60 seconds (1 minute)
 * to 1,209,600 seconds (14 days). The default value is 345,600 seconds (4 days).
 */
class MessageRetentionPeriod(unsafe: String) : ValidatedInt(validator, unsafe) {
    companion object {
        private val validator = IntValidator.interval(60, 1_209_600)
    }
}