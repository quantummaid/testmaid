package de.quantummaid.monolambda.model.modes.jvm

import de.quantummaid.monolambda.MonoLambdaDefinition
import de.quantummaid.monolambda.model.modes.graalvm.MonoModeRuntimeAwsLambda
import de.quantummaid.monolambda.model.modes.graalvm.MonoRuntimeNotInitializedException


fun MonoLambdaDefinition.monoModeJvm(): MonoModeJvm {
    TODO()
}


class MonoModeJvm() {
    fun enterRuntime(): MonoModeRuntimeAwsLambda {
        TODO()
    }
}

class JvmLambdaInvocationHandler {
    private val runtime = MONO_MODE?.enterRuntime() ?: throw MonoRuntimeNotInitializedException()

    companion object {
        private var MONO_MODE: MonoModeJvm? = null

        fun initMode(monoLambdaDefinition: MonoLambdaDefinition) {
            MONO_MODE = monoLambdaDefinition.monoModeJvm()
        }
    }

    fun handleRequest(event: Map<String, Any?>?): MutableMap<String, Any>? {
        return runtime.handleRequest(event)
    }
}
