package de.quantummaid.monolambda.awssdk.common

import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.apache.ApacheHttpClient

data class ConfiguredSdkHttpClient(val client: SdkHttpClient)

class ConfiguredSdkHttpClientFactory public constructor() {
    fun sdkHttpClient(): ConfiguredSdkHttpClient {
        val client = ApacheHttpClient.builder().build()
        return ConfiguredSdkHttpClient(client)
    }
}
