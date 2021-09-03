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

import de.quantummaid.monolambda.cf.parts.lambda.GraalVmFunction
import de.quantummaid.monolambda.cf.parts.lambda.LambdaFunctionMemorySize
import de.quantummaid.monolambda.cf.parts.lambda.LambdaFunctionTimeout


class MonoLambdaTemplate(
        val memorySize: LambdaFunctionMemorySize = LambdaFunctionMemorySize("256"),
        val timeout: LambdaFunctionTimeout = LambdaFunctionTimeout("20"),
        val parts: Collection<CloudFormationTemplatePart>
) {
    fun render(): String {
        val intel = CloudFormationTemplateIntel(
                lambdaFunctionResourceId = "MonoLambdaFunction",
                namespaceParameterName = "Namespace",
                lambdaCodeS3BucketNameParameterName = "CodeS3BucketName",
                lambdaCodeS3ObjectKeyParameterName = "CodeS3ObjectKey"
        )

        val header = """
            ---
            AWSTemplateFormatVersion: '2010-09-09'
            Description: "AWS Websocket Demo Lambda deployment."
            
            Parameters:
              ${intel.namespaceParameterName}:
                Type: String
                Description: "Unique string used to make resource identifiers unique"
              ${intel.lambdaCodeS3BucketNameParameterName}:
                Type: String
                Description: "The S3 bucket in the same AWS Region as your function containing your lambdas code."
              ${intel.lambdaCodeS3ObjectKeyParameterName}:
                Type: String
                Description: "The S3 object key of your lambdas code."
            
            Resources:
        """.trimIndent()
        val partsResources = parts.mapNotNull { it.resources(intel) }.joinToString("\n\n")
        val graalVmFunction = GraalVmFunction(memorySize, timeout)
        val monoLambdaCoreResources = graalVmFunction.resources(intel, parts)
        val resources = partsResources + "\n\n" + monoLambdaCoreResources

        val outputsTop = """
            Outputs:
        """.trimIndent()
        val partsOutputs = parts.mapNotNull { it.outputs(intel) }.joinToString("\n")
        val outputs = outputsTop + "\n" + partsOutputs

        return header + "\n" + resources + "\n" + outputs


    }
}