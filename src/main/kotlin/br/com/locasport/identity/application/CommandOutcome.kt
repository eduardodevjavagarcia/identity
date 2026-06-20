package br.com.locasport.identity.application

sealed interface CommandOutcome {
    data object Discarded : CommandOutcome

    data class Applied(
        val events: Int,
    ) : CommandOutcome
}
