package br.com.locasport.identity.adapters.outbound

import br.com.locasport.identity.adapters.config.EventStoreProperties
import br.com.locasport.identity.domain.AppendResult
import br.com.locasport.identity.domain.DomainEvent
import br.com.locasport.identity.domain.EventStore
import br.com.locasport.identity.domain.StoredEvent
import br.com.locasport.identity.domain.StreamId
import kotlinx.coroutines.flow.toList
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import org.springframework.r2dbc.core.flow
import java.time.ZoneOffset

class R2dbcEventStore(
    private val client: DatabaseClient,
    private val codec: AvroEventCodec,
    private val properties: EventStoreProperties,
) : EventStore {
    override suspend fun load(stream: StreamId): List<StoredEvent> =
        client
            .sql(
                "SELECT sequence_number, payload FROM ${properties.table} " +
                    "WHERE stream_id = :id ORDER BY sequence_number ASC",
            ).bind("id", stream.value)
            .map { row ->
                StoredEvent(
                    row.get("sequence_number", java.lang.Long::class.java)!!.toLong(),
                    codec.decode(row.get("payload", ByteArray::class.java)!!),
                )
            }.flow()
            .toList()

    override suspend fun append(
        stream: StreamId,
        expectedVersion: Long,
        events: List<DomainEvent>,
    ): AppendResult {
        var sequence = expectedVersion
        for (event in events) {
            sequence += 1
            client
                .sql(
                    "INSERT INTO ${properties.table} " +
                        "(stream_id, sequence_number, event_type, payload, occurred_at) " +
                        "VALUES (:id, :seq, :type, :payload, :at)",
                ).bind("id", stream.value)
                .bind("seq", sequence)
                .bind("type", event.type)
                .bind("payload", codec.encode(event))
                .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                .await()
        }
        return AppendResult(sequence)
    }
}
