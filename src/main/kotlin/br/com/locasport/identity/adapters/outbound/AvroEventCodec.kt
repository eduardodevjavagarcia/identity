package br.com.locasport.identity.adapters.outbound

import br.com.locasport.identity.domain.AssuranceLevel
import br.com.locasport.identity.domain.AssuranceLevelRaised
import br.com.locasport.identity.domain.DomainEvent
import br.com.locasport.identity.domain.EventId
import br.com.locasport.identity.domain.LegalBasis
import br.com.locasport.identity.domain.PartnerDeactivated
import br.com.locasport.identity.domain.PartnerId
import br.com.locasport.identity.domain.PartnerIdentityVerified
import br.com.locasport.identity.domain.PartnerReactivated
import br.com.locasport.identity.domain.PartnerRegistered
import br.com.locasport.identity.domain.PartnerRejected
import br.com.locasport.identity.domain.PartnerReviewSubmitted
import br.com.locasport.identity.domain.PartnerSuspended
import br.com.locasport.identity.domain.PartnerType
import br.com.locasport.identity.domain.PersonActivated
import br.com.locasport.identity.domain.PersonDeactivated
import br.com.locasport.identity.domain.PersonId
import br.com.locasport.identity.domain.PersonReactivated
import br.com.locasport.identity.domain.PersonRegistered
import br.com.locasport.identity.domain.PersonSuspended
import br.com.locasport.identity.domain.PurposeDisclosed
import br.com.locasport.identity.domain.Role
import br.com.locasport.identity.domain.RoleGranted
import br.com.locasport.identity.domain.RoleRevoked
import org.apache.avro.Schema
import org.apache.avro.SchemaNormalization
import org.apache.avro.message.BinaryMessageEncoder
import org.apache.avro.specific.SpecificRecordBase
import java.nio.ByteBuffer
import java.nio.ByteOrder
import br.com.locasport.identity.schema.avro.AssuranceLevel as AvroAssuranceLevel
import br.com.locasport.identity.schema.avro.AssuranceLevelRaised as AvroAssuranceLevelRaised
import br.com.locasport.identity.schema.avro.LegalBasis as AvroLegalBasis
import br.com.locasport.identity.schema.avro.PartnerDeactivated as AvroPartnerDeactivated
import br.com.locasport.identity.schema.avro.PartnerIdentityVerified as AvroPartnerIdentityVerified
import br.com.locasport.identity.schema.avro.PartnerReactivated as AvroPartnerReactivated
import br.com.locasport.identity.schema.avro.PartnerRegistered as AvroPartnerRegistered
import br.com.locasport.identity.schema.avro.PartnerRejected as AvroPartnerRejected
import br.com.locasport.identity.schema.avro.PartnerReviewSubmitted as AvroPartnerReviewSubmitted
import br.com.locasport.identity.schema.avro.PartnerSuspended as AvroPartnerSuspended
import br.com.locasport.identity.schema.avro.PartnerType as AvroPartnerType
import br.com.locasport.identity.schema.avro.PersonActivated as AvroPersonActivated
import br.com.locasport.identity.schema.avro.PersonDeactivated as AvroPersonDeactivated
import br.com.locasport.identity.schema.avro.PersonReactivated as AvroPersonReactivated
import br.com.locasport.identity.schema.avro.PersonRegistered as AvroPersonRegistered
import br.com.locasport.identity.schema.avro.PersonSuspended as AvroPersonSuspended
import br.com.locasport.identity.schema.avro.PurposeDisclosed as AvroPurposeDisclosed
import br.com.locasport.identity.schema.avro.Role as AvroRole
import br.com.locasport.identity.schema.avro.RoleGranted as AvroRoleGranted
import br.com.locasport.identity.schema.avro.RoleRevoked as AvroRoleRevoked

class AvroEventCodec {
    private val decoders: Map<Long, (ByteArray) -> DomainEvent> =
        mapOf(
            fingerprint(AvroPersonRegistered.getClassSchema()) to
                { bytes -> fromAvro(AvroPersonRegistered.getDecoder().decode(bytes)) },
            fingerprint(AvroPersonActivated.getClassSchema()) to
                { bytes -> fromAvro(AvroPersonActivated.getDecoder().decode(bytes)) },
            fingerprint(AvroPersonSuspended.getClassSchema()) to
                { bytes -> fromAvro(AvroPersonSuspended.getDecoder().decode(bytes)) },
            fingerprint(AvroPersonReactivated.getClassSchema()) to
                { bytes -> fromAvro(AvroPersonReactivated.getDecoder().decode(bytes)) },
            fingerprint(AvroPersonDeactivated.getClassSchema()) to
                { bytes -> fromAvro(AvroPersonDeactivated.getDecoder().decode(bytes)) },
            fingerprint(AvroPartnerRegistered.getClassSchema()) to
                { bytes -> fromAvro(AvroPartnerRegistered.getDecoder().decode(bytes)) },
            fingerprint(AvroPartnerReviewSubmitted.getClassSchema()) to
                { bytes -> fromAvro(AvroPartnerReviewSubmitted.getDecoder().decode(bytes)) },
            fingerprint(AvroPartnerIdentityVerified.getClassSchema()) to
                { bytes -> fromAvro(AvroPartnerIdentityVerified.getDecoder().decode(bytes)) },
            fingerprint(AvroPartnerRejected.getClassSchema()) to
                { bytes -> fromAvro(AvroPartnerRejected.getDecoder().decode(bytes)) },
            fingerprint(AvroPartnerSuspended.getClassSchema()) to
                { bytes -> fromAvro(AvroPartnerSuspended.getDecoder().decode(bytes)) },
            fingerprint(AvroPartnerReactivated.getClassSchema()) to
                { bytes -> fromAvro(AvroPartnerReactivated.getDecoder().decode(bytes)) },
            fingerprint(AvroPartnerDeactivated.getClassSchema()) to
                { bytes -> fromAvro(AvroPartnerDeactivated.getDecoder().decode(bytes)) },
            fingerprint(AvroPurposeDisclosed.getClassSchema()) to
                { bytes -> fromAvro(AvroPurposeDisclosed.getDecoder().decode(bytes)) },
            fingerprint(AvroAssuranceLevelRaised.getClassSchema()) to
                { bytes -> fromAvro(AvroAssuranceLevelRaised.getDecoder().decode(bytes)) },
            fingerprint(AvroRoleGranted.getClassSchema()) to
                { bytes -> fromAvro(AvroRoleGranted.getDecoder().decode(bytes)) },
            fingerprint(AvroRoleRevoked.getClassSchema()) to
                { bytes -> fromAvro(AvroRoleRevoked.getDecoder().decode(bytes)) },
        )

    fun encode(event: DomainEvent): ByteArray =
        when (event) {
            is PersonRegistered -> serialize(AvroPersonRegistered.getEncoder(), toAvro(event))
            is PersonActivated -> serialize(AvroPersonActivated.getEncoder(), toAvro(event))
            is PersonSuspended -> serialize(AvroPersonSuspended.getEncoder(), toAvro(event))
            is PersonReactivated -> serialize(AvroPersonReactivated.getEncoder(), toAvro(event))
            is PersonDeactivated -> serialize(AvroPersonDeactivated.getEncoder(), toAvro(event))
            is PartnerRegistered -> serialize(AvroPartnerRegistered.getEncoder(), toAvro(event))
            is PartnerReviewSubmitted -> serialize(AvroPartnerReviewSubmitted.getEncoder(), toAvro(event))
            is PartnerIdentityVerified -> serialize(AvroPartnerIdentityVerified.getEncoder(), toAvro(event))
            is PartnerRejected -> serialize(AvroPartnerRejected.getEncoder(), toAvro(event))
            is PartnerSuspended -> serialize(AvroPartnerSuspended.getEncoder(), toAvro(event))
            is PartnerReactivated -> serialize(AvroPartnerReactivated.getEncoder(), toAvro(event))
            is PartnerDeactivated -> serialize(AvroPartnerDeactivated.getEncoder(), toAvro(event))
            is PurposeDisclosed -> serialize(AvroPurposeDisclosed.getEncoder(), toAvro(event))
            is AssuranceLevelRaised -> serialize(AvroAssuranceLevelRaised.getEncoder(), toAvro(event))
            is RoleGranted -> serialize(AvroRoleGranted.getEncoder(), toAvro(event))
            is RoleRevoked -> serialize(AvroRoleRevoked.getEncoder(), toAvro(event))
            else -> throw IllegalArgumentException("unsupported event ${event.type}")
        }

    fun decode(payload: ByteArray): DomainEvent {
        val fingerprint = fingerprintOf(payload)
        val decoder = decoders[fingerprint] ?: throw IllegalArgumentException("unknown schema fingerprint $fingerprint")
        return decoder(payload)
    }

    private fun toAvro(event: PersonRegistered): AvroPersonRegistered =
        AvroPersonRegistered(
            event.personId.value,
            AvroLegalBasis.valueOf(event.legalBasis.name),
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: PersonActivated): AvroPersonActivated =
        AvroPersonActivated(
            event.personId.value,
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: PersonSuspended): AvroPersonSuspended =
        AvroPersonSuspended(
            event.personId.value,
            event.reason,
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: PersonReactivated): AvroPersonReactivated =
        AvroPersonReactivated(
            event.personId.value,
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: PersonDeactivated): AvroPersonDeactivated =
        AvroPersonDeactivated(
            event.personId.value,
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: PartnerRegistered): AvroPartnerRegistered =
        AvroPartnerRegistered(
            event.partnerId.value,
            AvroPartnerType.valueOf(event.partnerType.name),
            event.legalName,
            event.taxId,
            AvroLegalBasis.valueOf(event.legalBasis.name),
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: PartnerReviewSubmitted): AvroPartnerReviewSubmitted =
        AvroPartnerReviewSubmitted(
            event.partnerId.value,
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: PartnerIdentityVerified): AvroPartnerIdentityVerified =
        AvroPartnerIdentityVerified(
            event.partnerId.value,
            event.legalName,
            event.taxId,
            AvroPartnerType.valueOf(event.partnerType.name),
            AvroAssuranceLevel.valueOf(event.assuranceLevel.name),
            event.verifiedAt,
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: PartnerRejected): AvroPartnerRejected =
        AvroPartnerRejected(
            event.partnerId.value,
            event.reason,
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: PartnerSuspended): AvroPartnerSuspended =
        AvroPartnerSuspended(
            event.partnerId.value,
            event.reason,
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: PartnerReactivated): AvroPartnerReactivated =
        AvroPartnerReactivated(
            event.partnerId.value,
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: PartnerDeactivated): AvroPartnerDeactivated =
        AvroPartnerDeactivated(
            event.partnerId.value,
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: PurposeDisclosed): AvroPurposeDisclosed =
        AvroPurposeDisclosed(
            event.subjectId,
            event.purpose,
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: AssuranceLevelRaised): AvroAssuranceLevelRaised =
        AvroAssuranceLevelRaised(
            event.subjectId,
            AvroAssuranceLevel.valueOf(event.from.name),
            AvroAssuranceLevel.valueOf(event.to.name),
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: RoleGranted): AvroRoleGranted =
        AvroRoleGranted(
            event.subjectId,
            AvroRole.valueOf(event.role.name),
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: RoleRevoked): AvroRoleRevoked =
        AvroRoleRevoked(
            event.subjectId,
            AvroRole.valueOf(event.role.name),
            event.eventId.value,
            event.occurredAt,
        )

    private fun fromAvro(record: AvroPersonRegistered): PersonRegistered =
        PersonRegistered(
            PersonId(record.personId),
            LegalBasis.valueOf(record.legalBasis.name),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroPersonActivated): PersonActivated =
        PersonActivated(
            PersonId(record.personId),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroPersonSuspended): PersonSuspended =
        PersonSuspended(
            PersonId(record.personId),
            record.reason,
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroPersonReactivated): PersonReactivated =
        PersonReactivated(
            PersonId(record.personId),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroPersonDeactivated): PersonDeactivated =
        PersonDeactivated(
            PersonId(record.personId),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroPartnerRegistered): PartnerRegistered =
        PartnerRegistered(
            PartnerId(record.partnerId),
            PartnerType.valueOf(record.partnerType.name),
            record.legalName,
            record.taxId,
            LegalBasis.valueOf(record.legalBasis.name),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroPartnerReviewSubmitted): PartnerReviewSubmitted =
        PartnerReviewSubmitted(
            PartnerId(record.partnerId),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroPartnerIdentityVerified): PartnerIdentityVerified =
        PartnerIdentityVerified(
            PartnerId(record.partnerId),
            record.legalName,
            record.taxId,
            PartnerType.valueOf(record.partnerType.name),
            AssuranceLevel.valueOf(record.assuranceLevel.name),
            record.verifiedAt,
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroPartnerRejected): PartnerRejected =
        PartnerRejected(
            PartnerId(record.partnerId),
            record.reason,
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroPartnerSuspended): PartnerSuspended =
        PartnerSuspended(
            PartnerId(record.partnerId),
            record.reason,
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroPartnerReactivated): PartnerReactivated =
        PartnerReactivated(
            PartnerId(record.partnerId),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroPartnerDeactivated): PartnerDeactivated =
        PartnerDeactivated(
            PartnerId(record.partnerId),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroPurposeDisclosed): PurposeDisclosed =
        PurposeDisclosed(
            record.subjectId,
            record.purpose,
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroAssuranceLevelRaised): AssuranceLevelRaised =
        AssuranceLevelRaised(
            record.subjectId,
            AssuranceLevel.valueOf(record.from.name),
            AssuranceLevel.valueOf(record.to.name),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroRoleGranted): RoleGranted =
        RoleGranted(
            record.subjectId,
            Role.valueOf(record.role.name),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroRoleRevoked): RoleRevoked =
        RoleRevoked(
            record.subjectId,
            Role.valueOf(record.role.name),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun <T : SpecificRecordBase> serialize(
        encoder: BinaryMessageEncoder<T>,
        datum: T,
    ): ByteArray {
        val buffer = encoder.encode(datum)
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return bytes
    }

    private fun fingerprint(schema: Schema): Long = SchemaNormalization.parsingFingerprint64(schema)

    private fun fingerprintOf(payload: ByteArray): Long {
        require(payload.size >= HEADER_SIZE && payload[0] == MARKER_FIRST && payload[1] == MARKER_SECOND) {
            "invalid avro single-object header"
        }
        return ByteBuffer.wrap(payload, 2, 8).order(ByteOrder.LITTLE_ENDIAN).long
    }

    private companion object {
        const val HEADER_SIZE = 10
        const val MARKER_FIRST = 0xC3.toByte()
        const val MARKER_SECOND = 0x01.toByte()
    }
}
