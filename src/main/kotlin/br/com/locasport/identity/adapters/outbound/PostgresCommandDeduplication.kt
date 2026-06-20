package br.com.locasport.identity.adapters.outbound

import br.com.locasport.identity.domain.CommandDeduplicationPort
import br.com.locasport.identity.domain.CommandId
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitRowsUpdated
import java.time.OffsetDateTime
import java.time.ZoneOffset

class PostgresCommandDeduplication(
    private val client: DatabaseClient,
) : CommandDeduplicationPort {
    override suspend fun firstSeen(commandId: CommandId): Boolean {
        val affected =
            client
                .sql(
                    "INSERT INTO processed_command (command_id, processed_at) VALUES (:id, :at) " +
                        "ON CONFLICT (command_id) DO NOTHING",
                ).bind("id", commandId.value)
                .bind("at", OffsetDateTime.now(ZoneOffset.UTC))
                .fetch()
                .awaitRowsUpdated()
        return affected > 0
    }
}
