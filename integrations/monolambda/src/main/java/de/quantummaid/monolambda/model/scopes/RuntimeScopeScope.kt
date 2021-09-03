package de.quantummaid.monolambda.model.scopes

import de.quantummaid.injectmaid.InjectMaid
import de.quantummaid.injectmaid.InjectMaidBuilder
import de.quantummaid.injectmaid.api.Injector
import de.quantummaid.monolambda.model.cfg.EnvVariables

private data class RuntimeScopeScope(val envVariables: EnvVariables)

fun InjectMaid.enterRuntimeScope(envVariables: EnvVariables): Injector {
    return this.enterScope(RuntimeScopeScope(envVariables))
}

fun InjectMaidBuilder.withinRuntimeScope(block: InjectMaidBuilder.() -> Unit): InjectMaidBuilder {
    this.withinBuildTimeScope {
        withScope(RuntimeScopeScope::class.java) { scopedBuilder ->
            scopedBuilder.withCustomType(EnvVariables::class.java, RuntimeScopeScope::class.java) { it.envVariables }
            block(scopedBuilder)
        }
    }
    return this
}