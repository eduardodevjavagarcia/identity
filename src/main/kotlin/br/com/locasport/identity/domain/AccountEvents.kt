package br.com.locasport.identity.domain

import java.time.Instant

data class AccountRegistered(
    val accountId: AccountId,
    val subjectType: SubjectType,
    val purposeReference: PurposeReference,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(accountId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "AccountRegistered"
    }
}

data class IdentityClaimSubmitted(
    val accountId: AccountId,
    val claimType: ClaimType,
    val purposeReference: PurposeReference,
    val legalBasisReference: LegalBasisReference,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(accountId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "IdentityClaimSubmitted"
    }
}

data class AssuranceLevelRaised(
    val accountId: AccountId,
    val from: AssuranceLevel,
    val to: AssuranceLevel,
    val purposeReference: PurposeReference,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(accountId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "AssuranceLevelRaised"
    }
}

data class RoleAssigned(
    val accountId: AccountId,
    val role: Role,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(accountId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "RoleAssigned"
    }
}

data class RoleRevoked(
    val accountId: AccountId,
    val role: Role,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(accountId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "RoleRevoked"
    }
}

data class AccountSuspended(
    val accountId: AccountId,
    val reason: String,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(accountId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "AccountSuspended"
    }
}

data class AccountReinstated(
    val accountId: AccountId,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(accountId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "AccountReinstated"
    }
}
