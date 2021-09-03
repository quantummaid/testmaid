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

package de.quantummaid.monolambda.cf

class ManagementTemplate() {
    fun render(): String {
        return """
            ---            
            AWSTemplateFormatVersion: 2010-09-09
            Description: S3 bucket required to run lambda.
            
            Parameters:
              ArtifactBucketName:
                Type: String
                Description: "S3 Bucket where jars are located"
            
            Resources:
              LambdaCodeArtifactBucket:
                Type: "AWS::S3::Bucket"
                Properties:
                  AccessControl: Private
                  BucketName: !Ref ArtifactBucketName
                  LifecycleConfiguration:
                    Rules:
                      - Status: Enabled
                        ExpirationInDays: 1
        """.trimIndent()
    }
}