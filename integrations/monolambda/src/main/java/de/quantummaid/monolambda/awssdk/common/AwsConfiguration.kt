package de.quantummaid.monolambda.awssdk.common

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.regions.Region

data class AwsConfiguration(
    val region: Region,
    val credentialsProvider: AwsCredentialsProvider,
    val configuredSdkHttpClient: ConfiguredSdkHttpClient
)

