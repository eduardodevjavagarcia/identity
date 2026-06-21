package br.com.locasport.identity.domain

import java.time.Instant

data class PersonRegistered(
    val personId: PersonId,
    val legalBasis: LegalBasis,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(personId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "PersonRegistered"
    }
}

data class PersonActivated(
    val personId: PersonId,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(personId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "PersonActivated"
    }
}

data class PersonSuspended(
    val personId: PersonId,
    val reason: String,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(personId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "PersonSuspended"
    }
}

data class PersonReactivated(
    val personId: PersonId,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(personId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "PersonReactivated"
    }
}

data class PersonDeactivated(
    val personId: PersonId,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId.of(personId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "PersonDeactivated"
    }
}
