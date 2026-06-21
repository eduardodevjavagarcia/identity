package br.com.locasport.identity.domain

import java.time.Instant
import java.util.UUID

data class PurposeDisclosed(
    val subjectId: UUID,
    val purpose: String,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId(subjectId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "PurposeDisclosed"
    }
}

data class AssuranceLevelRaised(
    val subjectId: UUID,
    val from: AssuranceLevel,
    val to: AssuranceLevel,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId(subjectId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "AssuranceLevelRaised"
    }
}

data class RoleGranted(
    val subjectId: UUID,
    val role: Role,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId(subjectId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "RoleGranted"
    }
}

data class RoleRevoked(
    val subjectId: UUID,
    val role: Role,
    override val eventId: EventId = EventId.random(),
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent {
    override val stream: StreamId get() = StreamId(subjectId)
    override val type: String get() = TYPE

    companion object {
        const val TYPE = "RoleRevoked"
    }
}
