package de.quantummaid.monolambda.model.scopes

class InjectMaidHolder(private val injectMaid: de.quantummaid.injectmaid.InjectMaid) {
    init {
        nullableInjectMaidRef = this.injectMaid
    }

    companion object {
        private var nullableInjectMaidRef: de.quantummaid.injectmaid.InjectMaid? = null
        val injectMaidRef: de.quantummaid.injectmaid.InjectMaid
            get() {
                return nullableInjectMaidRef!!
            }
    }
}
