package de.quantummaid.monolambda.awssdk.dynamodb

import de.quantummaid.monolambda.awssdk.common.AwsConfiguration
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

class DynamoDbClientFactory {
    fun dynamoDbClient(awsConfiguration: AwsConfiguration): DynamoDbClient {
        val dynamoDbClient = DynamoDbClient.builder()
            .region(awsConfiguration.region)
            .credentialsProvider(awsConfiguration.credentialsProvider)
            .httpClient(awsConfiguration.configuredSdkHttpClient.client)
            .build()

        dynamoDbClient.listTables {
            it.limit(1)
        }

        return dynamoDbClient
    }
}
