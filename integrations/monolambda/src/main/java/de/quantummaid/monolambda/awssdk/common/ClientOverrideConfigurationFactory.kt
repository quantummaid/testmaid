package de.quantummaid.monolambda.awssdk.common

import software.amazon.awssdk.awscore.client.config.AwsAdvancedClientOption
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration

class ClientOverrideConfigurationFactory {
    fun clientOverrideConfiguration(): ClientOverrideConfiguration {
        return ClientOverrideConfiguration.builder()
            .defaultProfileFile(EmptyProfileFileBuilder.buildEmptyProfile())
            .putAdvancedOption(AwsAdvancedClientOption.ENABLE_DEFAULT_REGION_DETECTION, false)
                //TODO: How to disable that???
//            .option(SdkClientOption.ENDPOINT_DISCOVERY_ENABLED, false)
            //.option(SdkClientOption.CLIENT_TYPE, ClientType.SYNC)
            //.option(SdkClientOption.PROFILE_FILE)
            .build()
    }
}
