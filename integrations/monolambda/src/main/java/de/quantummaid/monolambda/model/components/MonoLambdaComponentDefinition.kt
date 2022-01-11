package de.quantummaid.monolambda.model.components

import de.quantummaid.monolambda.ConfigurationDefinition

interface MonoLambdaComponentDefinition {
    companion object {
        data class Conflict(val description: String) {}
    }

    fun conflicts(other: MonoLambdaComponentDefinition): Conflict?
    fun configurationDefinition(): ConfigurationDefinition
}
