package de.quantummaid.monolambda.model.scopes

import de.quantummaid.injectmaid.InjectMaid
import de.quantummaid.injectmaid.InjectMaidBuilder
import de.quantummaid.injectmaid.api.Injector
import de.quantummaid.monolambda.model.TraceId

data class InvocationRequest(val event: Map<String?, Any?>?)

private data class InvocationScope(
        val invocationRequest: InvocationRequest,
        val traceId: TraceId
)

fun InjectMaid.enterInvocationScope(event: Map<String?, Any?>?, traceId: TraceId): Injector {
    return this.enterScope(InvocationScope(InvocationRequest(event), traceId))
}

fun InjectMaidBuilder.withinInvocationScope(block: InjectMaidBuilder.() -> Unit): InjectMaidBuilder {
    this.withinRuntimeScope {
        withScope(InvocationScope::class.java) { scopedBuilder ->
            scopedBuilder.withCustomType(InvocationRequest::class.java, InvocationScope::class.java) { it.invocationRequest }
            scopedBuilder.withCustomType(TraceId::class.java, InvocationScope::class.java) { it.traceId }
            block(scopedBuilder)
        }
    }
    return this
}
