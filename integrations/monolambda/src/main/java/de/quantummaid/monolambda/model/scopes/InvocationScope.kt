package de.quantummaid.monolambda.model.scopes

import de.quantummaid.injectmaid.InjectMaid
import de.quantummaid.injectmaid.InjectMaidBuilder
import de.quantummaid.injectmaid.api.Injector
import de.quantummaid.monolambda.model.cfg.EnvVariables

private data class InvocationScope(val envVariables: EnvVariables)

fun InjectMaid.enterInvocationScope(envVariables: EnvVariables): Injector {
    return this.enterScope(InvocationScope(envVariables))
}

fun InjectMaidBuilder.withinInvocationScope(block: InjectMaidBuilder.() -> Unit): InjectMaidBuilder {
    this.withinRuntimeScope {
        withScope(InvocationScope::class.java) { scopedBuilder ->
            scopedBuilder.withCustomType(EnvVariables::class.java, InvocationScope::class.java) { it.envVariables }
            block(scopedBuilder)
        }
    }
    return this
}