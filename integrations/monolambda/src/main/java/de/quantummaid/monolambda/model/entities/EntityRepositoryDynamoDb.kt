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

package de.quantummaid.monolambda.model.entities

import de.quantummaid.monolambda.awssdk.dynamodb.DynamoDbTransaction
import software.amazon.awssdk.services.dynamodb.model.AttributeValue


class EntityRepositoryDynamoDb<Entity : Any>(val adapter: EntityAdapter<Entity>,
                                             val config: EntityTableConfiguration<Entity>,
                                             val transaction: DynamoDbTransaction
) {
    fun create(entity: Entity) {
        val serializedEntity = adapter.toDynamoDbItemMap(entity)

        transaction.put {
            tableName(tableName())
            item(serializedEntity)
            conditionExpression("attribute_not_exists(${idPropertyName()})")
        }
    }

    fun update(entity: Entity): Entity {
        val currentMvccId = adapter.mvccId(entity)
        val nextEntity = adapter.incrementMvccId(entity)
        val serialized = adapter.toDynamoDbItemMap(nextEntity)

        transaction.put {
            tableName(tableName())
            item(serialized)
            conditionExpression("attribute_exists(${idPropertyName()}) and ${mvccIdPropertyName()} = :currentmvccid")
            expressionAttributeValues(
                    mapOf(
                            ":currentmvccid" to AttributeValue
                                    .builder()
                                    .s(currentMvccId)
                                    .build(),
                    )
            )
        }
        return nextEntity
    }

    fun delete(entity: Entity) {
        val currentMvccId = adapter.mvccId(entity)

        transaction.delete {
            tableName(tableName())
            key(mapOf(
                    idPropertyName() to AttributeValue.builder().s(adapter.id(entity)).build(),
            ))
            conditionExpression("attribute_exists(${idPropertyName()}) and ${mvccIdPropertyName()} = :currentmvccid")
                    .expressionAttributeValues(
                            mapOf(
                                    ":currentmvccid" to AttributeValue
                                            .builder()
                                            .s(currentMvccId)
                                            .build(),
                            )
                    )
        }
    }


    /**
     * This is not paged and will cause trouble with many items. For now, there is no feature that would cause reads
     * of >1k results so we can delay implementing paging for a little bit longer. Based on our understanding atm
     * paging in dynamoDB is not possible, the only possible solution to deal with many items is endless scrolling.
     */
    fun findAll(): List<Entity> {
        val client = this.transaction.client
        val resultIterable = client.scanPaginator {
            it
                    .tableName(tableName())
                    .consistentRead(true)
        }
        val items = resultIterable.items()
                .map {
                    adapter.toEntity(it)
                }
        return items
    }

    private fun tableName() = config.tableName.mappingValue()

    private fun mvccIdPropertyName() = config.mvccIdPropertyName.mappingValue()

    private fun idPropertyName() = config.idPropertyName.mappingValue()
}
