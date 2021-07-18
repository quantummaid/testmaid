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

import de.quantummaid.mapmaid.validatedtypeskotlin.ValidationException
import de.quantummaid.testmaid.localfs.Md5Sum
import de.quantummaid.testmaid.localfs.Md5Sum.Companion.md5SumOf

/**
 * https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/cloudformation-limits.html
 */
data class Parameters(val parameters: List<Pair<ParameterKey, ParameterValue>>) {
    companion object {
        fun parameters(vararg values: Pair<String, String>): Parameters {
            return Parameters(values.map { ParameterKey(it.first) to ParameterValue(it.second) })
        }
    }

    init {
        if (parameters.size > 200) {
            throw ValidationException("must contain between 0 and 200 parameters")
        }
    }

    fun md5Sum(): Md5Sum {
        val joinToString = parameters.joinToString("") { it.first.mappingValue() + it.second.mappingValue() }
        return md5SumOf(joinToString)
    }

}

