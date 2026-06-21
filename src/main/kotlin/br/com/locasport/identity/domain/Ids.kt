package br.com.locasport.identity.domain

import java.util.UUID

@JvmInline
value class StreamId(
    val value: UUID,
) {
    companion object {
        fun of(personId: PersonId): StreamId = StreamId(personId.value)

        fun of(partnerId: PartnerId): StreamId = StreamId(partnerId.value)

        fun of(subjectId: UUID): StreamId = StreamId(subjectId)
    }
}

@JvmInline
value class EventId(
    val value: UUID,
) {
    companion object {
        fun random(): EventId = EventId(UUID.randomUUID())
    }
}

@JvmInline
value class CommandId(
    val value: UUID,
)

@JvmInline
value class PersonId(
    val value: UUID,
) {
    companion object {
        fun random(): PersonId = PersonId(UUID.randomUUID())

        fun unassigned(): PersonId = PersonId(UUID(0L, 0L))
    }
}

@JvmInline
value class PartnerId(
    val value: UUID,
) {
    companion object {
        fun random(): PartnerId = PartnerId(UUID.randomUUID())

        fun unassigned(): PartnerId = PartnerId(UUID(0L, 0L))
    }
}
