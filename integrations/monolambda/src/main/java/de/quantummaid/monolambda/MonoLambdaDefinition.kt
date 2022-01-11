package de.quantummaid.monolambda

import de.quantummaid.monolambda.cf.parts.lambda.LambdaFunctionMemorySize
import de.quantummaid.monolambda.cf.parts.lambda.LambdaFunctionTimeout
import de.quantummaid.monolambda.model.MonoLambdaName

data class MonoLambdaDefinition(
        val name: MonoLambdaName,
        val memorySize: LambdaFunctionMemorySize,
        val timeout: LambdaFunctionTimeout,
) {


}
