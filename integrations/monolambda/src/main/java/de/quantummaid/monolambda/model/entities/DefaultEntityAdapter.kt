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

import de.quantummaid.mapmaid.MapMaid
import de.quantummaid.mapmaid.builder.MapMaidBuilder
import de.quantummaid.mapmaid.dynamodb.attributevalue.AttributeValueMarshallerAndUnmarshaller
import de.quantummaid.mapmaid.validatedtypeskotlin.types.ValueType
import de.quantummaid.mapmaid.validatedtypeskotlin.withSupportForMapMaidValidatedTypes
import de.quantummaid.monolambda.cf.parts.dynamodb.DynamoDbPropertyName
import de.quantummaid.reflectmaid.Executor
import de.quantummaid.reflectmaid.ReflectMaid
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class DefaultEntityAdapter<Entity : Any>(
        private val entityType: ResolvedType,
        private val idExtractor: Executor,
        private val mvccIdExtractor: Executor,
        private val mvccIdIncrementer: Executor,
        private val mapMaid: MapMaid,
) : EntityAdapter<Entity> {
    companion object {
        fun <Entity : Any> buildFromConventionalEntity(
                entityType: KClass<Entity>,
                reflectMaid: ReflectMaid,
                mapMaidBuilder: MapMaidBuilder,
        ): DefaultEntityAdapter<Entity> {
            val resolvedEntityType = reflectMaid.resolve(entityType)

            val idExtractor: Executor = resolvedEntityType.methods().single { it.name() == "getId" }.createExecutor()
            val mvccIdExtractor: Executor = resolvedEntityType.methods().single { it.name() == "getMvccId" }.createExecutor()
            val incrementerMethod = resolvedEntityType.methods().single {
                it.name() == "incrementMvccId" && it.returnType == resolvedEntityType
            }

            val mvccIdIncrementer: Executor = incrementerMethod.createExecutor()

            val mapMaid = mapMaidBuilder
                    .serializingAndDeserializing(entityType.java)
                    .withSupportForMapMaidValidatedTypes()
                    .withAdvancedSettings {
                        it.usingMarshaller(AttributeValueMarshallerAndUnmarshaller.attributeValueMarshallerAndUnmarshaller())
                    }
                    .build()

            return DefaultEntityAdapter(resolvedEntityType, idExtractor, mvccIdExtractor, mvccIdIncrementer, mapMaid)
        }
    }

    override fun id(entity: Entity): String {
        val methodInvocationResult = idExtractor.execute(entity, listOf())
        val casted = methodInvocationResult as ValueType<String>
        return casted.mappingValue()
    }

    override fun mvccId(entity: Entity): String {
        val methodInvocationResult = mvccIdExtractor.execute(entity, listOf())
        val casted = methodInvocationResult as ValueType<String>
        return casted.mappingValue()
    }

    override fun incrementMvccId(entity: Entity): Entity {
        return mvccIdIncrementer.execute(entity, listOf()) as Entity
    }

    override fun toDynamoDbItemMap(entity: Entity): Map<String, AttributeValue> {
        return mapMaid.serializeTo(entity, AttributeValueMarshallerAndUnmarshaller.DYNAMODB_ATTRIBUTEVALUE).m()
    }

    override fun toEntity(dynamoDbItemMap: Map<String, AttributeValue>): Entity {
        val attributeValue: AttributeValue = AttributeValue.builder().m(dynamoDbItemMap).build()
        return mapMaid.deserialize(attributeValue, entityType.assignableType(), AttributeValueMarshallerAndUnmarshaller.DYNAMODB_ATTRIBUTEVALUE) as Entity
    }
}
