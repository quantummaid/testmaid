package de.quantummaid.monolambda.awssdk.common

import de.quantummaid.injectmaid.InjectMaidBuilder
import de.quantummaid.injectmaid.api.InjectorConfiguration
import de.quantummaid.injectmaid.api.ReusePolicy
import de.quantummaid.monolambda.model.scopes.withinRuntimeScope
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.regions.Region

fun InjectMaidBuilder.withAwsConfiguration(): InjectMaidBuilder {
    this.withConfiguration(AwsConfigurationInjectorConfiguration())
    return this
}

private class AwsConfigurationInjectorConfiguration : InjectorConfiguration {
    override fun apply(builder: InjectMaidBuilder) {
        builder
                .withFactory(
                        ClientOverrideConfiguration::class.java,
                        ClientOverrideConfigurationFactory::class.java,
                        ReusePolicy.EAGER_SINGLETON
                )
                .withinRuntimeScope {
                    withFactory(Region::class.java, AwsRegionFactory::class.java, ReusePolicy.EAGER_SINGLETON)
                    withFactory(
                            AwsCredentialsProvider::class.java,
                            AwsCredentialsProviderFactory::class.java,
                            ReusePolicy.EAGER_SINGLETON
                    )
                    withFactory(
                            ConfiguredSdkHttpClient::class.java, ConfiguredSdkHttpClientFactory::class.java,
                            ReusePolicy.EAGER_SINGLETON
                    )
                    withFactory(
                            AwsConfiguration::class.java, AwsConfigurationFactory::class.java,
                            ReusePolicy.EAGER_SINGLETON
                    )
                }
    }
}

