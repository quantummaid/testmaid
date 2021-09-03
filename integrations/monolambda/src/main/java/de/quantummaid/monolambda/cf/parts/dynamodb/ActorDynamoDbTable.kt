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

package de.quantummaid.monolambda.cf.parts.dynamodb

import de.quantummaid.monolambda.cf.CloudFormationTemplateIntel
import de.quantummaid.monolambda.cf.CloudFormationTemplatePart
import de.quantummaid.monolambda.model.ActorName

data class ActorDynamoDbTable(
        val actorName: ActorName,
        val actorIdDynamoDbPropertyName: DynamoDbPropertyName
) : CloudFormationTemplatePart {
    override fun resources(intel: CloudFormationTemplateIntel): String {
        return """
            ${actorTableResourceId()}:
              Type: AWS::DynamoDB::Table
              Properties:
                TableName:  ${tableName(intel)}
                BillingMode: PAY_PER_REQUEST
                AttributeDefinitions:
                  - AttributeName: "${actorIdDynamoDbPropertyName.mappingValue()}"
                    AttributeType: "S"
                  - AttributeName: "messageIndex"
                    AttributeType: "N"
                KeySchema:
                  - AttributeName: "${actorIdDynamoDbPropertyName.mappingValue()}"
                    KeyType: "HASH"
                  - AttributeName: "messageIndex"
                    KeyType: "RANGE"
                Tags:
                  - Key: ${intel.namespaceParameterName}
                    Value: !Ref ${intel.namespaceParameterName}
          """.replaceIndent("  ")
    }

    override fun envVars(intel: CloudFormationTemplateIntel): String {
        return """
            "ACTOR_TABLE_${actorName.upperCased()}": !Ref ${actorTableResourceId()}
        """.replaceIndent("          ")
    }

    override fun policy(intel: CloudFormationTemplateIntel): String {
        return """
            - PolicyName: ${intel.namespacePrefixedString("${actorName.mappingValue()}ActorTablePolicy")}
              PolicyDocument:
                Version: '2012-10-17'
                Statement:
                  - Action:
                      - 'dynamodb:DeleteItem'
                      - 'dynamodb:GetItem'
                      - 'dynamodb:PutItem'
                      - 'dynamodb:Query'
                      - 'dynamodb:Scan'
                      - 'dynamodb:UpdateItem'
                      - 'dynamodb:ConditionCheckItem'
                    Resource:
                      - !GetAtt ${actorTableResourceId()}.Arn
                    Effect: Allow
        """.replaceIndent("        ")
    }

    override fun outputs(intel: CloudFormationTemplateIntel): String {
        return """
            ActorTable${actorName.mappingValue()}:
              Value: !Ref ${actorTableResourceId()}
        """.replaceIndent("  ")
    }

    private fun tableName(intel: CloudFormationTemplateIntel): String {
        val namespacePrefixed = intel.namespacePrefixedString(actorTableResourceId().mappingValue())
        return namespacePrefixed
    }

    private fun actorTableResourceId() = DynamoDbTableName("""${actorName.mappingValue()}ActorTable""")
}