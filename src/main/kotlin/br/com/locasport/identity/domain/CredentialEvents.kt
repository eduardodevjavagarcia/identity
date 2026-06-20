package br.com.locasport.identity.domain

import java.time.Instant

data class CredentialRegistered(
    val credentialId: CredentialId,
    val accountId: AccountId,
    val factorType: FactorType,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(credentialId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "CredentialRegistered"
    }
}

data class CredentialActivated(
    val credentialId: CredentialId,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(credentialId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "CredentialActivated"
    }
}

data class StepUpChallengeCompleted(
    val credentialId: CredentialId,
    val accountId: AccountId,
    val achievedAssurance: AssuranceLevel,
    val purposeReference: PurposeReference,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(credentialId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "StepUpChallengeCompleted"
    }
}

data class CredentialRevoked(
    val credentialId: CredentialId,
    val reason: String,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(credentialId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "CredentialRevoked"
    }
}
