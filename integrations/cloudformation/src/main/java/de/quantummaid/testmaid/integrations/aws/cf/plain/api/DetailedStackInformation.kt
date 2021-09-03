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

data class DetailedStackInformation(
    val stackName: StackName,
    val stackId: StackId,
    val outputs: Set<Pair<OutputName, OutputValue>>
) {
    init {
        if (outputs.size > 200) {
            throw ValidationException("must not contain more than 200 parameters")
        }
    }

    operator fun get(outputName: OutputName): OutputValue {
        return (outputs.singleOrNull { it.first == outputName } ?: throw UnsupportedOperationException(
            "no '$outputName' in stack '$stackName'[${stackId}] (available outputs: ${outputs})"
        )).second
    }
}
