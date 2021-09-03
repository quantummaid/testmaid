package de.quantummaid.monolambda.awssdk.common

import org.apache.http.client.CredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider

class AwsCredentialsProviderFactory {
    fun awsCredentialsProvider(): AwsCredentialsProvider {
//        val credentialsProvider: AwsCredentialsProvider = EnvironmentVariableCredentialsProvider.create()
        val credentialsProvider: AwsCredentialsProvider = DefaultCredentialsProvider.create()
        return credentialsProvider
    }
}
