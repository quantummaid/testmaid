package my.company.mymono.infra

import de.quantummaid.monolambda.model.modes.graalvm.GraalVmMain

/**
 * When building Graal, state that this class has to be initialized
 */
class GraalModeInitializer {
    companion object {
        init {
            GraalVmMain.initMode(MonoConfiguration.configureMonoLambda())
        }
    }
}
