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
import de.quantummaid.monolambda.model.entities.EntityName

data class EntityDynamoDbTable(
        val entityName: EntityName,
        val entityIdDynamoDbPropertyName: DynamoDbPropertyName
) : CloudFormationTemplatePart {
    override fun resources(intel: CloudFormationTemplateIntel): String {
        return """
            ${entityTableResourceId()}:
              Type: AWS::DynamoDB::Table
              Properties:
                TableName: ${tableName(intel).mappingValue()}
                BillingMode: PAY_PER_REQUEST
                AttributeDefinitions:
                  - AttributeName: "${entityIdDynamoDbPropertyName.mappingValue()}"
                    AttributeType: "S"
                KeySchema:
                  - AttributeName: "${entityIdDynamoDbPropertyName.mappingValue()}"
                    KeyType: "HASH"
                Tags:
                  - Key: ${intel.namespaceParameterName}
                    Value: !Ref ${intel.namespaceParameterName}
          """.replaceIndent("  ")
    }

    override fun envVars(intel: CloudFormationTemplateIntel): String {
        return """
            "ENTITY_TABLE_${entityName.upperCased()}": !Ref ${entityTableResourceId()}
        """.replaceIndent("          ")
    }

    override fun policy(intel: CloudFormationTemplateIntel): String {
        return """
            - PolicyName: ${intel.namespacePrefixedString("${entityName.mappingValue()}EntityTablePolicy")}
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
                      - !GetAtt ${entityTableResourceId()}.Arn
                    Effect: Allow
        """.replaceIndent("        ")
    }

    override fun outputs(intel: CloudFormationTemplateIntel): String {
        return """
            EntityTable${entityName.mappingValue()}:
              Value: !Ref ${entityTableResourceId()}
        """.replaceIndent("  ")
    }

    private fun tableName(intel: CloudFormationTemplateIntel): DynamoDbTableName {
        return DynamoDbTableName(intel.namespacePrefixedString(entityTableResourceId()))
    }

    private fun entityTableResourceId() = """${entityName.mappingValue()}EntityTable"""
}