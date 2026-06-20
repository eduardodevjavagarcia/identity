package br.com.locasport.identity.adapters.outbound

import br.com.locasport.identity.domain.AccountId
import br.com.locasport.identity.domain.AccountRegistered
import br.com.locasport.identity.domain.AccountReinstated
import br.com.locasport.identity.domain.AccountSuspended
import br.com.locasport.identity.domain.AssuranceLevel
import br.com.locasport.identity.domain.AssuranceLevelRaised
import br.com.locasport.identity.domain.DomainEvent
import br.com.locasport.identity.domain.ProjectionStore
import br.com.locasport.identity.domain.RoleAssigned
import br.com.locasport.identity.domain.RoleRevoked
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import org.springframework.r2dbc.core.awaitOneOrNull
import java.time.ZoneOffset

class AccountProjection(
    private val client: DatabaseClient,
) : ProjectionStore {
    override suspend fun apply(events: List<DomainEvent>) {
        for (event in events) {
            when (event) {
                is AccountRegistered ->
                    client
                        .sql(
                            "INSERT INTO account_view " +
                                "(account_id, subject_type, status, assurance, roles, updated_at) " +
                                "VALUES (:id, :subjectType, 'REGISTERED', 'NONE', '', :at) " +
                                "ON CONFLICT (account_id) DO UPDATE SET " +
                                "subject_type = EXCLUDED.subject_type, status = EXCLUDED.status, " +
                                "updated_at = EXCLUDED.updated_at",
                        ).bind("id", event.accountId.value)
                        .bind("subjectType", event.subjectType.name)
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .await()

                is AssuranceLevelRaised -> {
                    val status =
                        when (event.to) {
                            AssuranceLevel.HIGH -> "VERIFIED"
                            AssuranceLevel.SUBSTANTIAL -> "IDENTIFIED"
                            else -> "REGISTERED"
                        }
                    client
                        .sql(
                            "UPDATE account_view SET assurance = :assurance, status = :status, " +
                                "updated_at = :at WHERE account_id = :id",
                        ).bind("assurance", event.to.name)
                        .bind("status", status)
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.accountId.value)
                        .await()
                }

                is AccountSuspended ->
                    client
                        .sql("UPDATE account_view SET status = 'SUSPENDED', updated_at = :at WHERE account_id = :id")
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.accountId.value)
                        .await()

                is AccountReinstated ->
                    client
                        .sql(
                            "UPDATE account_view SET status = " +
                                "CASE WHEN assurance IN ('SUBSTANTIAL', 'HIGH') THEN 'IDENTIFIED' ELSE 'REGISTERED' END, " +
                                "updated_at = :at WHERE account_id = :id",
                        ).bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.accountId.value)
                        .await()

                is RoleAssigned -> {
                    val roles = currentRoles(event.accountId)
                    roles.add(event.role.name)
                    client
                        .sql("UPDATE account_view SET roles = :roles, updated_at = :at WHERE account_id = :id")
                        .bind("roles", roles.sorted().joinToString(","))
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.accountId.value)
                        .await()
                }

                is RoleRevoked -> {
                    val roles = currentRoles(event.accountId)
                    roles.remove(event.role.name)
                    client
                        .sql("UPDATE account_view SET roles = :roles, updated_at = :at WHERE account_id = :id")
                        .bind("roles", roles.sorted().joinToString(","))
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.accountId.value)
                        .await()
                }

                else -> Unit
            }
        }
    }

    private suspend fun currentRoles(accountId: AccountId): MutableSet<String> {
        val serialized =
            client
                .sql("SELECT roles FROM account_view WHERE account_id = :id")
                .bind("id", accountId.value)
                .map { row -> row.get("roles", String::class.java) ?: "" }
                .awaitOneOrNull()
                ?: ""
        return serialized
            .split(",")
            .filter { it.isNotBlank() }
            .toMutableSet()
    }
}
