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

package my.company.mymono

import de.quantummaid.mapmaid.MapMaid
import de.quantummaid.monolambda.MonoLambdaDefinition
import de.quantummaid.monolambda.cf.parts.lambda.LambdaFunctionMemorySize
import de.quantummaid.monolambda.cf.parts.lambda.LambdaFunctionTimeout
import de.quantummaid.monolambda.model.MonoLambdaName
import de.quantummaid.monolambda.model.entities.DefaultEntityAdapter
import de.quantummaid.monolambda.model.entities.EntityName
import de.quantummaid.monolambda.model.entities.EntityUseCaseAdapter
import de.quantummaid.monolambda.model.entities.MonoEntityDefinition
import de.quantummaid.monolambda.model.logging.MonoLambdaLoggingDefinition
import de.quantummaid.monolambda.model.tests.fullremote.systemUnderTestFullRemote
import de.quantummaid.reflectmaid.ReflectMaid
import my.company.mymono.user.model.User
import my.company.mymono.user.usecases.create.CreateUser
import my.company.mymono.user.usecases.list.ListUsers


fun main() {
    val reflectMaid = ReflectMaid.aReflectMaid()
    val mapMaidBuilder = MapMaid.aMapMaid()

    val components = listOf(
            MonoLambdaLoggingDefinition(),
            MonoEntityDefinition(
                    EntityName("User"),
                    DefaultEntityAdapter.buildFromConventionalEntity(User::class, reflectMaid, mapMaidBuilder),
                    listOf(
                            ListUsers::class,
                            CreateUser::class
                    )
            ),
    )
    val definition = MonoLambdaDefinition(
            MonoLambdaName("UserManager"),
            LambdaFunctionMemorySize("256"),
            LambdaFunctionTimeout("20"),
            components
    )
    val sut = definition.systemUnderTestFullRemote()

}

