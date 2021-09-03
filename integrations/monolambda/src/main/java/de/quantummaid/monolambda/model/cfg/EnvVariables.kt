package de.quantummaid.monolambda.model.cfg

class EnvVariables(private val environment: Map<String, String>) {
    companion object {
        fun fromSystem(): EnvVariables {
            return EnvVariables(System.getenv())
        }
    }

    fun byName(name: String): String? {
        return environment[name]
    }

    fun requiredByName(name: String): String {
        return environment[name] ?: error("Key ${name} not found in environment ${environment}")
    }

    fun byNameOrDefault(name: String, defaultValue: String): String {
        return byName(name) ?: defaultValue
    }

    fun istSet(name: String): Boolean {
        return byName(name) != null
    }

    fun all(): Map<String, String> {
        return environment
    }

    fun hasValue(
        name: String,
        value: String
    ): Boolean {
        return value == environment[name]
    }
}
