package br.com.locasport.identity.domain

import java.time.Instant

data class PartnerRegistered(
    val partnerId: PartnerId,
    val partnerType: PartnerType,
    val legalName: String,
    val taxId: String,
    val legalBasis: LegalBasis,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(partnerId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "PartnerRegistered"
    }
}

data class PartnerReviewSubmitted(
    val partnerId: PartnerId,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(partnerId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "PartnerReviewSubmitted"
    }
}

data class PartnerIdentityVerified(
    val partnerId: PartnerId,
    val legalName: String,
    val taxId: String,
    val partnerType: PartnerType,
    val assuranceLevel: AssuranceLevel,
    val verifiedAt: Instant,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(partnerId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "PartnerIdentityVerified"
    }
}

data class PartnerRejected(
    val partnerId: PartnerId,
    val reason: String,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(partnerId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "PartnerRejected"
    }
}

data class PartnerSuspended(
    val partnerId: PartnerId,
    val reason: String,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(partnerId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "PartnerSuspended"
    }
}

data class PartnerReactivated(
    val partnerId: PartnerId,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(partnerId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "PartnerReactivated"
    }
}

data class PartnerDeactivated(
    val partnerId: PartnerId,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(partnerId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "PartnerDeactivated"
    }
}
