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

package de.quantummaid.monolambda.cf.parts.lambda

import de.quantummaid.monolambda.cf.CloudFormationTemplateIntel
import de.quantummaid.monolambda.cf.CloudFormationTemplatePart

class GraalVmFunction(private val lambdaFunctionMemorySize: LambdaFunctionMemorySize,
                      private val lambdaFunctionTimeout: LambdaFunctionTimeout) {
    fun resources(intel: CloudFormationTemplateIntel, parts: Collection<CloudFormationTemplatePart>): String {
        /*
        TODO: JVM Version
         */

        val functionTopPart = """
            ${intel.lambdaFunctionResourceId}:
              Type: AWS::Lambda::Function
              Properties:
                FunctionName: ${intel.namespacePrefixedString(intel.lambdaFunctionResourceId)}
                Code:
                  S3Bucket: !Ref ${intel.lambdaCodeS3BucketNameParameterName}
                  S3Key: !Ref ${intel.lambdaCodeS3ObjectKeyParameterName}
                Tags:
                  - Key: ${intel.namespaceParameterName}
                    Value: !Ref ${intel.namespaceParameterName}
                MemorySize: ${lambdaFunctionMemorySize.mappingValue()}
                Handler: somethingThatIsIgnoredInTheQuantumMaidLambdaRuntime
                Role: !GetAtt ${intel.lambdaFunctionResourceId}Role.Arn
                Timeout: ${lambdaFunctionTimeout.mappingValue()}
                Runtime: provided
                Environment:
                  Variables:
        """.replaceIndent("  ")
        val functionEnvVariables = parts.mapNotNull { it.envVars(intel) }.joinToString("\n")
        val functionPart = functionTopPart + "\n" + functionEnvVariables
        val roleTopPart = """            
            ${intel.lambdaFunctionResourceId}Role:
              Type: AWS::IAM::Role
              Properties:
                RoleName: ${intel.namespacePrefixedString("${intel.lambdaFunctionResourceId}Role")}
                AssumeRolePolicyDocument:
                  Version: '2012-10-17'
                  Statement:
                    - Action:
                        - sts:AssumeRole
                      Effect: Allow
                      Principal:
                        Service:
                          - lambda.amazonaws.com
                ManagedPolicyArns:
                  - arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
                Tags:
                  - Key: ${intel.namespaceParameterName}
                    Value: !Ref ${intel.namespaceParameterName}
                Policies:
        """.replaceIndent("  ")
        val rolePoliciesPart = parts.mapNotNull { it.policy(intel) }.joinToString("\n")
        val rolePart = roleTopPart + "\n" + rolePoliciesPart
        return functionPart + "\n\n" + rolePart
    }
}