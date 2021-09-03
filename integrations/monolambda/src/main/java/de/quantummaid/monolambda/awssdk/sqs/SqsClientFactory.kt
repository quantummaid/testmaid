package de.quantummaid.monolambda.awssdk.sqs

import de.quantummaid.monolambda.awssdk.common.AwsConfiguration
import software.amazon.awssdk.services.sqs.SqsClient

class SqsClientFactory {
    fun sqsClient(awsConfiguration: AwsConfiguration): SqsClient {
        val sqsClient = SqsClient.builder()
            .region(awsConfiguration.region)
            .credentialsProvider(awsConfiguration.credentialsProvider)
            .httpClient(awsConfiguration.configuredSdkHttpClient.client)
            .build()

        sqsClient.listQueues { builder -> builder.maxResults(0) }

        return sqsClient
    }
}
