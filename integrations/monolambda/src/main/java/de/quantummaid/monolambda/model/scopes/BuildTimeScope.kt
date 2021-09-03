package de.quantummaid.monolambda.model.scopes

import de.quantummaid.injectmaid.InjectMaid
import de.quantummaid.injectmaid.InjectMaidBuilder
import de.quantummaid.injectmaid.api.Injector
import de.quantummaid.reflectmaid.ReflectMaid

private class BuildTimeScope()

fun InjectMaid.enterBuildTimeScope(): Injector {
    return this.enterScope(BuildTimeScope())
}

fun InjectMaidBuilder.withinBuildTimeScope(block: InjectMaidBuilder.() -> Unit): InjectMaidBuilder {
    this.withScope(BuildTimeScope::class.java) { scopedBuilder ->
        withCustomType(ReflectMaid::class.java, InjectMaid::class.java, InjectMaid::reflectMaid)
        block(scopedBuilder)
    }
    return this
}