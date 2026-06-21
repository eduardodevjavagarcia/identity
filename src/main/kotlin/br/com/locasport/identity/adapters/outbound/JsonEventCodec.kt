package br.com.locasport.identity.adapters.outbound

import br.com.locasport.identity.domain.AssuranceLevelRaised
import br.com.locasport.identity.domain.DomainEvent
import br.com.locasport.identity.domain.PartnerDeactivated
import br.com.locasport.identity.domain.PartnerIdentityVerified
import br.com.locasport.identity.domain.PartnerReactivated
import br.com.locasport.identity.domain.PartnerRegistered
import br.com.locasport.identity.domain.PartnerRejected
import br.com.locasport.identity.domain.PartnerReviewSubmitted
import br.com.locasport.identity.domain.PartnerSuspended
import br.com.locasport.identity.domain.PersonActivated
import br.com.locasport.identity.domain.PersonDeactivated
import br.com.locasport.identity.domain.PersonReactivated
import br.com.locasport.identity.domain.PersonRegistered
import br.com.locasport.identity.domain.PersonSuspended
import br.com.locasport.identity.domain.PurposeDisclosed
import br.com.locasport.identity.domain.RoleGranted
import br.com.locasport.identity.domain.RoleRevoked
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
            is PersonRegistered ->
                mapOf(
                    "personId" to event.personId.value.toString(),
                    "legalBasis" to event.legalBasis.name,
                )

            is PersonActivated ->
                mapOf(
                    "personId" to event.personId.value.toString(),
                )

            is PersonSuspended ->
                mapOf(
                    "personId" to event.personId.value.toString(),
                    "reason" to event.reason,
                )

            is PersonReactivated ->
                mapOf(
                    "personId" to event.personId.value.toString(),
                )

            is PersonDeactivated ->
                mapOf(
                    "personId" to event.personId.value.toString(),
                )

            is PartnerRegistered ->
                mapOf(
                    "partnerId" to event.partnerId.value.toString(),
                    "partnerType" to event.partnerType.name,
                    "legalName" to event.legalName,
                    "taxId" to event.taxId,
                    "legalBasis" to event.legalBasis.name,
                )

            is PartnerReviewSubmitted ->
                mapOf(
                    "partnerId" to event.partnerId.value.toString(),
                )

            is PartnerIdentityVerified ->
                mapOf(
                    "partnerId" to event.partnerId.value.toString(),
                    "legalName" to event.legalName,
                    "taxId" to event.taxId,
                    "partnerType" to event.partnerType.name,
                    "assuranceLevel" to event.assuranceLevel.name,
                    "verifiedAt" to event.verifiedAt.toString(),
                )

            is PartnerRejected ->
                mapOf(
                    "partnerId" to event.partnerId.value.toString(),
                    "reason" to event.reason,
                )

            is PartnerSuspended ->
                mapOf(
                    "partnerId" to event.partnerId.value.toString(),
                    "reason" to event.reason,
                )

            is PartnerReactivated ->
                mapOf(
                    "partnerId" to event.partnerId.value.toString(),
                )

            is PartnerDeactivated ->
                mapOf(
                    "partnerId" to event.partnerId.value.toString(),
                )

            is PurposeDisclosed ->
                mapOf(
                    "subjectId" to event.subjectId.toString(),
                    "purpose" to event.purpose,
                )

            is AssuranceLevelRaised ->
                mapOf(
                    "subjectId" to event.subjectId.toString(),
                    "from" to event.from.name,
                    "to" to event.to.name,
                )

            is RoleGranted ->
                mapOf(
                    "subjectId" to event.subjectId.toString(),
                    "role" to event.role.name,
                )

            is RoleRevoked ->
                mapOf(
                    "subjectId" to event.subjectId.toString(),
                    "role" to event.role.name,
                )

            else -> emptyMap()
        }
}
