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

package de.quantummaid.testmaid.integrations.aws.cf.env.internal

import de.quantummaid.mapmaid.MapMaid
import de.quantummaid.mapmaid.standardtypeskotlin.withSupportForStandardKotlinTypes
import de.quantummaid.testmaid.integrations.aws.cf.env.Environment
import de.quantummaid.testmaid.integrations.aws.cf.env.EnvironmentDefinition
import de.quantummaid.testmaid.integrations.aws.cf.env.EnvironmentName
import de.quantummaid.testmaid.integrations.aws.TagKey
import de.quantummaid.testmaid.integrations.aws.TagValue
import de.quantummaid.testmaid.integrations.aws.cf.env.CloudFormationEnvironmentService
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.Body
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.ParameterKey
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.ParameterValue
import de.quantummaid.testmaid.integrations.aws.cf.plain.api.StackName
import de.quantummaid.testmaid.integrations.aws.s3.BucketName
import de.quantummaid.testmaid.integrations.aws.s3.ObjectKey
import de.quantummaid.testmaid.localfs.LocalDirectory
import de.quantummaid.testmaid.localfs.LocalFile

/**
 * Tip: Use the current test suites 'target' directory containing all it's classes. This can be achieved by using:
 * LocalDirectory.targetDirectoryOfClass(<A class within your test suite>::class.java)
 */
class CloudFormationEnvironmentServiceCaching(
    private val cacheBaseDirectory: LocalDirectory,
    private val delegate: CloudFormationEnvironmentService
) : CloudFormationEnvironmentService {
    companion object {
        private val mapMaid: MapMaid = MapMaid.aMapMaid()
            .withSupportForStandardKotlinTypes()
            .serializingAndDeserializing(Environment::class.java)
            .serializingAndDeserializingCustomPrimitive(
                LocalFile::class.java,
                LocalFile::mappingValue,
                ::LocalFile
            )
            .serializingAndDeserializingCustomPrimitive(
                BucketName::class.java,
                BucketName::mappingValue,
                ::BucketName
            )
            .serializingAndDeserializingCustomPrimitive(
                ObjectKey::class.java,
                ObjectKey::mappingValue,
                ::ObjectKey
            )
            .serializingAndDeserializingCustomPrimitive(
                Body::class.java,
                Body::mappingValue,
                ::Body
            )
            .serializingAndDeserializingCustomPrimitive(
                EnvironmentName::class.java,
                EnvironmentName::mappingValue,
                ::EnvironmentName
            )
            .serializingAndDeserializingCustomPrimitive(
                ParameterKey::class.java,
                ParameterKey::mappingValue,
                ::ParameterKey
            )
            .serializingAndDeserializingCustomPrimitive(
                ParameterValue::class.java,
                ParameterValue::mappingValue,
                ::ParameterValue
            )
            .serializingAndDeserializingCustomPrimitive(
                TagKey::class.java,
                TagKey::mappingValue,
                ::TagKey
            )
            .serializingAndDeserializingCustomPrimitive(
                TagValue::class.java,
                TagValue::mappingValue,
                ::TagValue
            )
            .serializingAndDeserializingCustomPrimitive(
                StackName::class.java,
                StackName::mappingValue,
                ::StackName
            )
            .build()
    }

    override fun createOrUpdate(environmentDefinition: EnvironmentDefinition): Environment {
        val md5OfEnvironment = environmentDefinition.md5Sum()
        val cacheFileName = "EnvironmentDefinitionCache_${environmentDefinition.name}_${md5OfEnvironment}.json"
        val cachedEnv: LocalFile? = cacheBaseDirectory.file(cacheFileName)
        return if (cachedEnv != null) {
            val stringContent = cachedEnv.stringContent(Charsets.UTF_8)
            val environment = mapMaid.deserializeJson(stringContent, Environment::class.java)
            environment
        } else {
            val environment = delegate.createOrUpdate(environmentDefinition)
            val json = mapMaid.serializeToJson(environment)
            cacheBaseDirectory.createFile(cacheFileName, json.toByteArray(Charsets.UTF_8))
            environment
        }
    }

    override fun delete(environmentDefinition: EnvironmentDefinition) {
        delegate.delete(environmentDefinition)
    }
}
