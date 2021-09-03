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

package de.quantummaid.monolambda.cf.parts.sqs

import de.quantummaid.monolambda.cf.CloudFormationTemplateIntel
import de.quantummaid.monolambda.cf.CloudFormationTemplatePart
import de.quantummaid.monolambda.model.MonoLambdaName

data class ActorFiFo(
        val name: MonoLambdaName,
        val messageRetentionPeriod: MessageRetentionPeriod,
        val visibilityTimeout: MessageVisibilityTimeout
) : CloudFormationTemplatePart {
    override fun resources(intel: CloudFormationTemplateIntel): String {
        return """
            ActorFiFo:
              Type: AWS::SQS::Queue
              Properties:
                QueueName: ${intel.namespacePrefixedString("${name.mappingValue()}Actor.fifo")}
                DelaySeconds: 0
                FifoQueue: true
                MessageRetentionPeriod: ${messageRetentionPeriod.mappingValue()}
                ReceiveMessageWaitTimeSeconds: 0
                VisibilityTimeout: ${visibilityTimeout.mappingValue()}
                Tags:
                  - Key: ${intel.namespaceParameterName}
                    Value: !Ref ${intel.namespaceParameterName}
                
            ActorFiFoEventSourceMapping:
              Type: AWS::Lambda::EventSourceMapping
              Properties:
                BatchSize: 1
                Enabled: true
                EventSourceArn: !GetAtt ActorFiFo.Arn
                FunctionName: !GetAtt ${intel.lambdaFunctionResourceId}.Arn
        """.replaceIndent("  ")
    }

    override fun envVars(intel: CloudFormationTemplateIntel): String {
        return """
            "ACTOR_FIFO_URL": !Ref ActorFiFo
        """.replaceIndent("          ")
    }

    /**
     * More info here: https://docs.aws.amazon.com/service-authorization/latest/reference/list_amazonsqs.html
     *
     * Permissions not granted:
     *  - AddPermission
     *  - CreateQueue
     *  - DeleteQueue
     *  - GetQueueAttributes
     *  - ListDeadLetterSourceQueues
     *  - ListQueueTags
     *  - ListQueues
     *  - PurgeQueue
     *  - SetQueueAttributes
     *  - TagQueue
     *  - UntagQueue
     */
    override fun policy(intel: CloudFormationTemplateIntel): String {
        return """
            - PolicyName: ${intel.namespacePrefixedString("${name.mappingValue()}ActorFiFoPolicy")}
              PolicyDocument:
                Version: '2012-10-17'
                Statement:
                  - Action:
                      - "sqs:ChangeMessageVisibility"
                      - "sqs:ChangeMessageVisibilityBatch"
                      - "sqs:GetQueueUrl"
                      - "sqs:SendMessage"
                      - "sqs:SendMessageBatch"
                      - "sqs:DeleteMessage"
                      - "sqs:DeleteMessageBatch"
                      - "sqs:ReceiveMessage"
                    Resource:
                      - !GetAtt ActorFiFo.Arn
                    Effect: Allow
        """.replaceIndent("        ")
    }

    override fun outputs(intel: CloudFormationTemplateIntel): String {
        return """
            ActorFiFoUrl:
              Value: !Ref ActorFiFo
            ActorFiFoEventSourceMappingId:
              Value: !Ref ActorFiFoEventSourceMapping
        """.replaceIndent("  ")
    }
}