package br.com.locasport.identity.domain

import java.time.Instant

interface DomainEvent {
    val stream: StreamId
    val eventId: EventId
    val type: String
    val occurredAt: Instant
}

data class StoredEvent(
    val sequenceNumber: Long,
    val event: DomainEvent,
)

data class AppendResult(
    val version: Long,
)
