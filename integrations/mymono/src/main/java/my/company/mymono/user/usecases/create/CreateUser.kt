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

package my.company.mymono.user.usecases.create

import my.company.mymono.user.model.User
import my.company.mymono.user.model.UserId
import my.company.mymono.user.model.UserMvccId
import my.company.mymono.user.usecases.UserDto
import my.company.mymono.user.usecases.UserRepository


class CreateUser(private val userRepository: UserRepository) {

    fun execute(req: CreateUserRequest): CreateUserResponse {
        val user = User(UserId.newUnique(), UserMvccId.initialMvccId(), req.username, req.userNickname)
        userRepository.create(user)
        return CreateUserResponse(UserDto.fromUser(user))
    }

}
