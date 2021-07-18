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

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.nio.file.Path

internal class S3ServiceImpl(private val s3Client: S3Client, private val logFacade: LogFacade) : S3Service {
    override fun upload(localFile: Path, bucketName: BucketName, objectKey: ObjectKey) {
        logFacade.infoUploadingToS3(localFile, bucketName, objectKey)

        val listObjectsResponse = s3Client.listObjects(
            ListObjectsRequest
                .builder()
                .bucket(bucketName.mappingValue())
                .prefix(objectKey.mappingValue())
                .build()
        )
        if (listObjectsResponse.contents().isEmpty()) {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucketName.mappingValue())
                    .key(objectKey.mappingValue())
                    .build(),
                localFile
            )
        }

        logFacade.infoUploadedToS3(localFile, bucketName, objectKey)
    }
}
