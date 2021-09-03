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

package my.company.mymono.user.infra

import de.quantummaid.mapmaid.MapMaid
import de.quantummaid.mapmaid.dynamodb.attributevalue.AttributeValueMarshallerAndUnmarshaller
import de.quantummaid.monolambda.cf.parts.dynamodb.DynamoDbTableName
import my.company.mymono.user.model.User
import my.company.mymono.user.usecases.UserRepository

class UserRepositoryDynamoDbTableName(unsafe: String) : DynamoDbTableName(unsafe)

class UserRepositoryDynamoDb(
        private val transaction: DynamoDbTransaction,
        private val tableName: UserRepositoryDynamoDbTableName,
        private val mapMaid: MapMaid
) : UserRepository {
    companion object {
        fun mapMaid(): MapMaid {
            return MapMaid.aMapMaid()
                    .serializingAndDeserializing(User::class.java)
                    .withAdvancedSettings {
                        it.usingMarshaller(AttributeValueMarshallerAndUnmarshaller.attributeValueMarshallerAndUnmarshaller())
                    }
                    //TODO
//                    .withSupportForMapMaidValidatedTypes()
                    .build()
        }
    }

    override fun create(user: User) {
        TODO()
    }

    override fun update(user: User): User {
        TODO()
    }

    override fun delete(user: User) {
        TODO()
    }

    override fun allUsers(): List<User> {
        TODO()
//
//        val client = this.transaction.dynamoDbClient
//        val resultIterable = client.scanPaginator {
//            it
//                    .tableName(tableName.mappingValue())
//                    .consistentRead(true)
//        }
//        val items = resultIterable.items()
//                .map {
//                    AttributeValue.builder().m(it).build()
//                }
//                .map { mapMaid.deserialize(it!!, User::class.java, DYNAMODB_ATTRIBUTEVALUE) }
//        return items
    }
}