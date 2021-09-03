package de.quantummaid.monolambda.awssdk.common

import de.quantummaid.monolambda.model.cfg.EnvVariables
import software.amazon.awssdk.regions.Region

class AwsRegionFactory {
    fun region(envVariables: EnvVariables): Region {
        return Region.of(envVariables.requiredByName("AWS_REGION"))
    }
}
