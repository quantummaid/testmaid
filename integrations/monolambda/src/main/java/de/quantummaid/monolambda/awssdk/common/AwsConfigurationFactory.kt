package de.quantummaid.monolambda.awssdk.common

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.regions.Region

class AwsConfigurationFactory {
    fun awsConfiguration(
            region: Region,
            credentialsProvider: AwsCredentialsProvider,
            configuredSdkHttpClient: ConfiguredSdkHttpClient
    ): AwsConfiguration {
        return AwsConfiguration(region, credentialsProvider, configuredSdkHttpClient)
    }
}