package br.com.locasport.identity.domain

interface EventStore {
    suspend fun load(stream: StreamId): List<StoredEvent>

    suspend fun append(
        stream: StreamId,
        expectedVersion: Long,
        events: List<DomainEvent>,
    ): AppendResult
}

interface EventPublisher {
    suspend fun publish(events: List<DomainEvent>)
}

interface CommandDeduplicationPort {
    suspend fun firstSeen(commandId: CommandId): Boolean
}

interface ProjectionStore {
    suspend fun apply(events: List<DomainEvent>)
}

interface ObjectStoragePort {
    suspend fun archive(
        key: String,
        payload: ByteArray,
    )
}
