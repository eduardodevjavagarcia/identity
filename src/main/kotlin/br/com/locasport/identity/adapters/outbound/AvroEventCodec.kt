package br.com.locasport.identity.adapters.outbound

import br.com.locasport.identity.domain.AccountId
import br.com.locasport.identity.domain.AccountRegistered
import br.com.locasport.identity.domain.AccountReinstated
import br.com.locasport.identity.domain.AccountSuspended
import br.com.locasport.identity.domain.AssuranceLevel
import br.com.locasport.identity.domain.AssuranceLevelRaised
import br.com.locasport.identity.domain.ClaimType
import br.com.locasport.identity.domain.CredentialActivated
import br.com.locasport.identity.domain.CredentialId
import br.com.locasport.identity.domain.CredentialRegistered
import br.com.locasport.identity.domain.CredentialRevoked
import br.com.locasport.identity.domain.DomainEvent
import br.com.locasport.identity.domain.EventId
import br.com.locasport.identity.domain.FactorType
import br.com.locasport.identity.domain.IdentityClaimSubmitted
import br.com.locasport.identity.domain.LegalBasisReference
import br.com.locasport.identity.domain.PurposeReference
import br.com.locasport.identity.domain.Role
import br.com.locasport.identity.domain.RoleAssigned
import br.com.locasport.identity.domain.RoleRevoked
import br.com.locasport.identity.domain.StepUpChallengeCompleted
import br.com.locasport.identity.domain.SubjectType
import org.apache.avro.Schema
import org.apache.avro.SchemaNormalization
import org.apache.avro.message.BinaryMessageEncoder
import org.apache.avro.specific.SpecificRecordBase
import java.nio.ByteBuffer
import java.nio.ByteOrder
import br.com.locasport.identity.schema.avro.AccountRegistered as AvroAccountRegistered
import br.com.locasport.identity.schema.avro.AccountReinstated as AvroAccountReinstated
import br.com.locasport.identity.schema.avro.AccountSuspended as AvroAccountSuspended
import br.com.locasport.identity.schema.avro.AssuranceLevel as AvroAssuranceLevel
import br.com.locasport.identity.schema.avro.AssuranceLevelRaised as AvroAssuranceLevelRaised
import br.com.locasport.identity.schema.avro.ClaimType as AvroClaimType
import br.com.locasport.identity.schema.avro.CredentialActivated as AvroCredentialActivated
import br.com.locasport.identity.schema.avro.CredentialRegistered as AvroCredentialRegistered
import br.com.locasport.identity.schema.avro.CredentialRevoked as AvroCredentialRevoked
import br.com.locasport.identity.schema.avro.FactorType as AvroFactorType
import br.com.locasport.identity.schema.avro.IdentityClaimSubmitted as AvroIdentityClaimSubmitted
import br.com.locasport.identity.schema.avro.Role as AvroRole
import br.com.locasport.identity.schema.avro.RoleAssigned as AvroRoleAssigned
import br.com.locasport.identity.schema.avro.RoleRevoked as AvroRoleRevoked
import br.com.locasport.identity.schema.avro.StepUpChallengeCompleted as AvroStepUpChallengeCompleted
import br.com.locasport.identity.schema.avro.SubjectType as AvroSubjectType

class AvroEventCodec {
    private val decoders: Map<Long, (ByteArray) -> DomainEvent> =
        mapOf(
            fingerprint(AvroAccountRegistered.getClassSchema()) to
                { bytes -> fromAvro(AvroAccountRegistered.getDecoder().decode(bytes)) },
            fingerprint(AvroIdentityClaimSubmitted.getClassSchema()) to
                { bytes -> fromAvro(AvroIdentityClaimSubmitted.getDecoder().decode(bytes)) },
            fingerprint(AvroAssuranceLevelRaised.getClassSchema()) to
                { bytes -> fromAvro(AvroAssuranceLevelRaised.getDecoder().decode(bytes)) },
            fingerprint(AvroRoleAssigned.getClassSchema()) to { bytes -> fromAvro(AvroRoleAssigned.getDecoder().decode(bytes)) },
            fingerprint(AvroRoleRevoked.getClassSchema()) to { bytes -> fromAvro(AvroRoleRevoked.getDecoder().decode(bytes)) },
            fingerprint(AvroAccountSuspended.getClassSchema()) to
                { bytes -> fromAvro(AvroAccountSuspended.getDecoder().decode(bytes)) },
            fingerprint(AvroAccountReinstated.getClassSchema()) to
                { bytes -> fromAvro(AvroAccountReinstated.getDecoder().decode(bytes)) },
            fingerprint(AvroCredentialRegistered.getClassSchema()) to
                { bytes -> fromAvro(AvroCredentialRegistered.getDecoder().decode(bytes)) },
            fingerprint(AvroCredentialActivated.getClassSchema()) to
                { bytes -> fromAvro(AvroCredentialActivated.getDecoder().decode(bytes)) },
            fingerprint(AvroStepUpChallengeCompleted.getClassSchema()) to
                { bytes -> fromAvro(AvroStepUpChallengeCompleted.getDecoder().decode(bytes)) },
            fingerprint(AvroCredentialRevoked.getClassSchema()) to
                { bytes -> fromAvro(AvroCredentialRevoked.getDecoder().decode(bytes)) },
        )

    fun encode(event: DomainEvent): ByteArray =
        when (event) {
            is AccountRegistered -> serialize(AvroAccountRegistered.getEncoder(), toAvro(event))
            is IdentityClaimSubmitted -> serialize(AvroIdentityClaimSubmitted.getEncoder(), toAvro(event))
            is AssuranceLevelRaised -> serialize(AvroAssuranceLevelRaised.getEncoder(), toAvro(event))
            is RoleAssigned -> serialize(AvroRoleAssigned.getEncoder(), toAvro(event))
            is RoleRevoked -> serialize(AvroRoleRevoked.getEncoder(), toAvro(event))
            is AccountSuspended -> serialize(AvroAccountSuspended.getEncoder(), toAvro(event))
            is AccountReinstated -> serialize(AvroAccountReinstated.getEncoder(), toAvro(event))
            is CredentialRegistered -> serialize(AvroCredentialRegistered.getEncoder(), toAvro(event))
            is CredentialActivated -> serialize(AvroCredentialActivated.getEncoder(), toAvro(event))
            is StepUpChallengeCompleted -> serialize(AvroStepUpChallengeCompleted.getEncoder(), toAvro(event))
            is CredentialRevoked -> serialize(AvroCredentialRevoked.getEncoder(), toAvro(event))
            else -> throw IllegalArgumentException("unsupported event ${event.type}")
        }

    fun decode(payload: ByteArray): DomainEvent {
        val fingerprint = fingerprintOf(payload)
        val decoder = decoders[fingerprint] ?: throw IllegalArgumentException("unknown schema fingerprint $fingerprint")
        return decoder(payload)
    }

    private fun toAvro(event: AccountRegistered): AvroAccountRegistered =
        AvroAccountRegistered(
            event.accountId.value,
            AvroSubjectType.valueOf(event.subjectType.name),
            event.purposeReference.value,
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: IdentityClaimSubmitted): AvroIdentityClaimSubmitted =
        AvroIdentityClaimSubmitted(
            event.accountId.value,
            AvroClaimType.valueOf(event.claimType.name),
            event.purposeReference.value,
            event.legalBasisReference.value,
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: AssuranceLevelRaised): AvroAssuranceLevelRaised =
        AvroAssuranceLevelRaised(
            event.accountId.value,
            AvroAssuranceLevel.valueOf(event.from.name),
            AvroAssuranceLevel.valueOf(event.to.name),
            event.purposeReference.value,
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: RoleAssigned): AvroRoleAssigned =
        AvroRoleAssigned(
            event.accountId.value,
            AvroRole.valueOf(event.role.name),
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: RoleRevoked): AvroRoleRevoked =
        AvroRoleRevoked(
            event.accountId.value,
            AvroRole.valueOf(event.role.name),
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: AccountSuspended): AvroAccountSuspended =
        AvroAccountSuspended(
            event.accountId.value,
            event.reason,
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: AccountReinstated): AvroAccountReinstated =
        AvroAccountReinstated(
            event.accountId.value,
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: CredentialRegistered): AvroCredentialRegistered =
        AvroCredentialRegistered(
            event.credentialId.value,
            event.accountId.value,
            AvroFactorType.valueOf(event.factorType.name),
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: CredentialActivated): AvroCredentialActivated =
        AvroCredentialActivated(
            event.credentialId.value,
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: StepUpChallengeCompleted): AvroStepUpChallengeCompleted =
        AvroStepUpChallengeCompleted(
            event.credentialId.value,
            event.accountId.value,
            AvroAssuranceLevel.valueOf(event.achievedAssurance.name),
            event.purposeReference.value,
            event.eventId.value,
            event.occurredAt,
        )

    private fun toAvro(event: CredentialRevoked): AvroCredentialRevoked =
        AvroCredentialRevoked(
            event.credentialId.value,
            event.reason,
            event.eventId.value,
            event.occurredAt,
        )

    private fun fromAvro(record: AvroAccountRegistered): AccountRegistered =
        AccountRegistered(
            AccountId(record.accountId),
            SubjectType.valueOf(record.subjectType.name),
            PurposeReference(record.purposeReference),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroIdentityClaimSubmitted): IdentityClaimSubmitted =
        IdentityClaimSubmitted(
            AccountId(record.accountId),
            ClaimType.valueOf(record.claimType.name),
            PurposeReference(record.purposeReference),
            LegalBasisReference(record.legalBasisReference),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroAssuranceLevelRaised): AssuranceLevelRaised =
        AssuranceLevelRaised(
            AccountId(record.accountId),
            AssuranceLevel.valueOf(record.from.name),
            AssuranceLevel.valueOf(record.to.name),
            PurposeReference(record.purposeReference),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroRoleAssigned): RoleAssigned =
        RoleAssigned(
            AccountId(record.accountId),
            Role.valueOf(record.role.name),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroRoleRevoked): RoleRevoked =
        RoleRevoked(
            AccountId(record.accountId),
            Role.valueOf(record.role.name),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroAccountSuspended): AccountSuspended =
        AccountSuspended(
            AccountId(record.accountId),
            record.reason,
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroAccountReinstated): AccountReinstated =
        AccountReinstated(
            AccountId(record.accountId),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroCredentialRegistered): CredentialRegistered =
        CredentialRegistered(
            CredentialId(record.credentialId),
            AccountId(record.accountId),
            FactorType.valueOf(record.factorType.name),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroCredentialActivated): CredentialActivated =
        CredentialActivated(
            CredentialId(record.credentialId),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroStepUpChallengeCompleted): StepUpChallengeCompleted =
        StepUpChallengeCompleted(
            CredentialId(record.credentialId),
            AccountId(record.accountId),
            AssuranceLevel.valueOf(record.achievedAssurance.name),
            PurposeReference(record.purposeReference),
            EventId(record.eventId),
            record.occurredAt,
        )

    private fun fromAvro(record: AvroCredentialRevoked): CredentialRevoked =
        CredentialRevoked(
            CredentialId(record.credentialId),
            record.reason,
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
