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

package de.quantummaid.monolambda.cf.parts.lambda

import de.quantummaid.mapmaid.validatedtypeskotlin.types.ValidatedInt
import de.quantummaid.mapmaid.validatedtypeskotlin.validation.IntValidator
import de.quantummaid.monolambda.cf.parts.sqs.MessageRetentionPeriod
import de.quantummaid.monolambda.cf.parts.sqs.MessageVisibilityTimeout

/**
 * The amount of time in SECONDS that Lambda allows a function to run before stopping it.
 * The minimum is 1, the maximum allowed value is 900 seconds.
 */
class LambdaFunctionTimeout(unsafe: String) : ValidatedInt(validators, unsafe) {
    companion object {
        private val validators = IntValidator.interval(1, 900)
    }

    /**
     * That number is related to retries and dead letter queues. That topic needs some testing and research. Going with
     * the default for now.
     */
    fun messageRetentionPeriod(): MessageRetentionPeriod {
        return MessageRetentionPeriod("345600")
    }

    /**
     * Set your queue visibility timeout to 6 times your function timeout, plus the MaximumBatchingWindowInSeconds
     *
     * The extra time allows for Lambda to retry if your function execution is throttled while your function is
     * processing a previous batch.
     *
     * This needs some research and testing, since mono lambda is not batching atm and we process 1 message at a time
     * we are going with some headroom for sqs event source and nothing more.
     */
    fun messageVisibilityTimeout(): MessageVisibilityTimeout {
        return MessageVisibilityTimeout("${safeValue + 10}")
    }
}
