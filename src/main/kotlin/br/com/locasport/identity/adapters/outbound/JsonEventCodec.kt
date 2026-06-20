package br.com.locasport.identity.adapters.outbound

import br.com.locasport.identity.domain.AccountRegistered
import br.com.locasport.identity.domain.AccountReinstated
import br.com.locasport.identity.domain.AccountSuspended
import br.com.locasport.identity.domain.AssuranceLevelRaised
import br.com.locasport.identity.domain.CredentialActivated
import br.com.locasport.identity.domain.CredentialRegistered
import br.com.locasport.identity.domain.CredentialRevoked
import br.com.locasport.identity.domain.DomainEvent
import br.com.locasport.identity.domain.IdentityClaimSubmitted
import br.com.locasport.identity.domain.RoleAssigned
import br.com.locasport.identity.domain.RoleRevoked
import br.com.locasport.identity.domain.StepUpChallengeCompleted
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object JsonEventCodec {
    private val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    fun encode(event: DomainEvent): String {
        val document =
            linkedMapOf<String, Any?>(
                "type" to event.type,
                "eventId" to event.eventId.value.toString(),
                "stream" to event.stream.value.toString(),
                "occurredAt" to event.occurredAt.toString(),
            )
        document.putAll(body(event))
        return mapper.writeValueAsString(document)
    }

    private fun body(event: DomainEvent): Map<String, Any?> =
        when (event) {
            is AccountRegistered ->
                mapOf(
                    "accountId" to event.accountId.value.toString(),
                    "subjectType" to event.subjectType.name,
                    "purposeReference" to event.purposeReference.value,
                )

            is IdentityClaimSubmitted ->
                mapOf(
                    "accountId" to event.accountId.value.toString(),
                    "claimType" to event.claimType.name,
                    "purposeReference" to event.purposeReference.value,
                    "legalBasisReference" to event.legalBasisReference.value,
                )

            is AssuranceLevelRaised ->
                mapOf(
                    "accountId" to event.accountId.value.toString(),
                    "from" to event.from.name,
                    "to" to event.to.name,
                    "purposeReference" to event.purposeReference.value,
                )

            is RoleAssigned ->
                mapOf(
                    "accountId" to event.accountId.value.toString(),
                    "role" to event.role.name,
                )

            is RoleRevoked ->
                mapOf(
                    "accountId" to event.accountId.value.toString(),
                    "role" to event.role.name,
                )

            is AccountSuspended ->
                mapOf(
                    "accountId" to event.accountId.value.toString(),
                    "reason" to event.reason,
                )

            is AccountReinstated ->
                mapOf(
                    "accountId" to event.accountId.value.toString(),
                )

            is CredentialRegistered ->
                mapOf(
                    "credentialId" to event.credentialId.value.toString(),
                    "accountId" to event.accountId.value.toString(),
                    "factorType" to event.factorType.name,
                )

            is CredentialActivated ->
                mapOf(
                    "credentialId" to event.credentialId.value.toString(),
                )

            is StepUpChallengeCompleted ->
                mapOf(
                    "credentialId" to event.credentialId.value.toString(),
                    "accountId" to event.accountId.value.toString(),
                    "achievedAssurance" to event.achievedAssurance.name,
                    "purposeReference" to event.purposeReference.value,
                )

            is CredentialRevoked ->
                mapOf(
                    "credentialId" to event.credentialId.value.toString(),
                    "reason" to event.reason,
                )

            else -> emptyMap()
        }
}
