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

data class StatelessActionQueue(
        val name: MonoLambdaName,
        val retentionPeriod: MessageRetentionPeriod,
        val visibilityTimeout: MessageVisibilityTimeout
) : CloudFormationTemplatePart {
    override fun resources(intel: CloudFormationTemplateIntel): String {
        return """
            StatelessActionQueue:
              Type: AWS::SQS::Queue
              Properties:
                QueueName: ${intel.namespacePrefixedString("${name.mappingValue()}StatelessActionQueue")}
                DelaySeconds: 0
                FifoQueue: false
                MessageRetentionPeriod: ${retentionPeriod.mappingValue()}
                ReceiveMessageWaitTimeSeconds: 0
                VisibilityTimeout: ${visibilityTimeout.mappingValue()}
                Tags:
                  - Key: ${intel.namespaceParameterName}
                    Value: !Ref ${intel.namespaceParameterName}
                
            StatelessActionQueueEventSourceMapping:
              Type: AWS::Lambda::EventSourceMapping
              Properties:
                BatchSize: 1
                Enabled: true
                EventSourceArn: !GetAtt StatelessActionQueue.Arn
                FunctionName: !GetAtt ${intel.lambdaFunctionResourceId}.Arn
        """.replaceIndent("  ")
    }

    override fun envVars(intel: CloudFormationTemplateIntel): String {
        return """
            "STATELESS_ACTION_QUEUE_URL": !Ref StatelessActionQueue
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
            - PolicyName: ${intel.namespacePrefixedString("${name.mappingValue()}StatelessActionQueuePolicy")}
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
                      - !GetAtt StatelessActionQueue.Arn
                    Effect: Allow
        """.replaceIndent("        ")
    }

    override fun outputs(intel: CloudFormationTemplateIntel): String {
        return """
            StatelessActionQueueUrl:
              Value: !Ref StatelessActionQueue
            StatelessActionQueueEventSourceMappingId:
              Value: !Ref StatelessActionQueueEventSourceMapping
        """.replaceIndent("  ")
    }
}