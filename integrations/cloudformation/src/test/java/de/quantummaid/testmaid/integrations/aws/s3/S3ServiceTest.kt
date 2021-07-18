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

package de.quantummaid.testmaid.integrations.aws.s3

import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.cloudformation.CloudFormationClient
import java.nio.file.Path

internal class SutLogFacade : LogFacade {
    override fun infoUploadingToS3(localFile: Path, bucketName: BucketName, objectKey: ObjectKey) {
        println("infoUploadingToS3")
    }

    override fun infoUploadedToS3(localFile: Path, bucketName: BucketName, objectKey: ObjectKey) {
        println("infoUploadedToS3")
    }
}

internal class S3ServiceTest {
    @Test
    internal fun testUpload() {
//        val bucketName = BucketName("S3ServiceTest")
//        val cloudFormationClient = CloudFormationClient.builder().build()
//        println("Client done")
//        val stacks = cloudFormationClient.listStacksPaginator {
//            it.stackStatusFilters(
//                StackStatus.values() - arrayOf(
//                    StackStatus.DELETE_COMPLETE, StackStatus.DELETE_IN_PROGRESS
//                ),
//
//                )
//        }.stackSummaries()
//        println("Stacks done")
//
//        stacks.forEach { existingStack ->
//            cloudFormationClient.deleteStack { it.stackName(existingStack.stackName()) }
//        }
//
//        cloudFormationClient.createStack {
//            it.stackName("S3ServiceTest")
//            it.templateBody(S3BucketCFTemplate(bucketName).buildTemplateBody())
//        }
//        val s3Client = S3Client.builder().build()
//        val sut = S3Service.s3Service(s3Client, SutLogFacade())

    }
}
