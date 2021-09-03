package de.quantummaid.monolambda.awssdk.sqs

import de.quantummaid.injectmaid.InjectMaidBuilder
import de.quantummaid.injectmaid.api.InjectorConfiguration
import de.quantummaid.injectmaid.api.ReusePolicy
import de.quantummaid.monolambda.model.scopes.withinRuntimeScope
import software.amazon.awssdk.services.sqs.SqsClient

class SqsInjectorConfiguration : InjectorConfiguration {
    override fun apply(builder: InjectMaidBuilder) {
        builder.withinRuntimeScope {
            withFactory(
                    SqsClient::class.java,
                    SqsClientFactory::class.java,
                    ReusePolicy.EAGER_SINGLETON
            )
        }
    }
}
