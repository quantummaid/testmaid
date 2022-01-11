package my.company.mymono.infra

import de.quantummaid.mapmaid.MapMaid
import de.quantummaid.monolambda.MonoLambdaDefinition
import de.quantummaid.monolambda.cf.parts.lambda.LambdaFunctionMemorySize
import de.quantummaid.monolambda.cf.parts.lambda.LambdaFunctionTimeout
import de.quantummaid.monolambda.model.MonoLambdaName
import de.quantummaid.monolambda.model.entities.DefaultEntityAdapter
import de.quantummaid.monolambda.model.entities.EntityName
import de.quantummaid.monolambda.model.entities.MonoEntityDefinition
import de.quantummaid.monolambda.model.logging.MonoLambdaLoggingDefinition
import de.quantummaid.reflectmaid.ReflectMaid
import my.company.mymono.user.model.User

class MonoConfiguration {
    companion object {
        fun configureMonoLambda(): MonoLambdaDefinition {
            val reflectMaid = ReflectMaid.aReflectMaid()
            val mapMaidBuilder = MapMaid.aMapMaid()

            val components = listOf(
                    MonoLambdaLoggingDefinition(),
                    MonoEntityDefinition(
                            EntityName("User"),
                            DefaultEntityAdapter.buildFromConventionalEntity(User::class, reflectMaid, mapMaidBuilder),
                            listOf(
//                            ListUsers::class,
//                            CreateUser::class
                            )
                    ),
            )
            val definition = listOf(
                    components

            )
            return MonoLambdaDefinition(
                    MonoLambdaName("UserManager"),
                    LambdaFunctionMemorySize("256"),
                    LambdaFunctionTimeout("20"),
            )
        }
    }
}
