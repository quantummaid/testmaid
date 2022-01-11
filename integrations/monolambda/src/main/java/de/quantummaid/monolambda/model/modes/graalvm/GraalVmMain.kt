package de.quantummaid.monolambda.model.modes.graalvm

import de.quantummaid.graalvmlambdaruntime.GraalVmLambdaRuntime
import de.quantummaid.monolambda.MonoLambdaDefinition

class GraalVmMain {
    companion object {
        private var MONO_MODE: MonoModeAwsLambda? = null

        fun initMode(monoLambdaDefinition: MonoLambdaDefinition) {
            MONO_MODE = monoLambdaDefinition.monoModeGraalVm()
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val runtime = MONO_MODE?.enterRuntime() ?: throw MonoRuntimeNotInitializedException()
            GraalVmLambdaRuntime.startGraalVmLambdaRuntime(true) { event ->
                val retNullable = runtime.handleRequest(event)
                retNullable ?: mapOf()
            }
        }
    }
}

class MonoRuntimeNotInitializedException : IllegalStateException(
        "mono runtime not initialized. Make sure you are building the graal vm image"
)
