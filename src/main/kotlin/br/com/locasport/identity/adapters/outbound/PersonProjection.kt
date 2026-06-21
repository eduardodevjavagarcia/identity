package br.com.locasport.identity.adapters.outbound

import br.com.locasport.identity.domain.AssuranceLevelRaised
import br.com.locasport.identity.domain.DomainEvent
import br.com.locasport.identity.domain.PersonActivated
import br.com.locasport.identity.domain.PersonDeactivated
import br.com.locasport.identity.domain.PersonReactivated
import br.com.locasport.identity.domain.PersonRegistered
import br.com.locasport.identity.domain.PersonSuspended
import br.com.locasport.identity.domain.ProjectionStore
import br.com.locasport.identity.domain.PurposeDisclosed
import br.com.locasport.identity.domain.RoleGranted
import br.com.locasport.identity.domain.RoleRevoked
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import org.springframework.r2dbc.core.awaitOneOrNull
import java.time.ZoneOffset

class PersonProjection(
    private val client: DatabaseClient,
) : ProjectionStore {
    override suspend fun apply(events: List<DomainEvent>) {
        for (event in events) {
            when (event) {
                is PersonRegistered ->
                    client
                        .sql(
                            "INSERT INTO person_view " +
                                "(person_id, status, assurance, roles, legal_basis, purpose_disclosed, updated_at) " +
                                "VALUES (:id, 'REGISTERED', 'NONE', '', :legalBasis, FALSE, :at) " +
                                "ON CONFLICT (person_id) DO UPDATE SET " +
                                "legal_basis = EXCLUDED.legal_basis, status = EXCLUDED.status, " +
                                "updated_at = EXCLUDED.updated_at",
                        ).bind("id", event.personId.value)
                        .bind("legalBasis", event.legalBasis.name)
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .await()

                is AssuranceLevelRaised ->
                    client
                        .sql(
                            "UPDATE person_view SET assurance = :assurance, " +
                                "updated_at = :at WHERE person_id = :id",
                        ).bind("assurance", event.to.name)
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.subjectId)
                        .await()

                is PurposeDisclosed ->
                    client
                        .sql(
                            "UPDATE person_view SET purpose_disclosed = TRUE, " +
                                "updated_at = :at WHERE person_id = :id",
                        ).bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.subjectId)
                        .await()

                is PersonActivated ->
                    client
                        .sql("UPDATE person_view SET status = 'ACTIVE', updated_at = :at WHERE person_id = :id")
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.personId.value)
                        .await()

                is PersonSuspended ->
                    client
                        .sql("UPDATE person_view SET status = 'SUSPENDED', updated_at = :at WHERE person_id = :id")
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.personId.value)
                        .await()

                is PersonReactivated ->
                    client
                        .sql("UPDATE person_view SET status = 'ACTIVE', updated_at = :at WHERE person_id = :id")
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.personId.value)
                        .await()

                is PersonDeactivated ->
                    client
                        .sql("UPDATE person_view SET status = 'DEACTIVATED', updated_at = :at WHERE person_id = :id")
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.personId.value)
                        .await()

                is RoleGranted -> {
                    val roles = currentRoles(event.subjectId)
                    roles.add(event.role.name)
                    client
                        .sql("UPDATE person_view SET roles = :roles, updated_at = :at WHERE person_id = :id")
                        .bind("roles", roles.sorted().joinToString(","))
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.subjectId)
                        .await()
                }

                is RoleRevoked -> {
                    val roles = currentRoles(event.subjectId)
                    roles.remove(event.role.name)
                    client
                        .sql("UPDATE person_view SET roles = :roles, updated_at = :at WHERE person_id = :id")
                        .bind("roles", roles.sorted().joinToString(","))
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.subjectId)
                        .await()
                }

                else -> Unit
            }
        }
    }

    private suspend fun currentRoles(personId: java.util.UUID): MutableSet<String> {
        val serialized =
            client
                .sql("SELECT roles FROM person_view WHERE person_id = :id")
                .bind("id", personId)
                .map { row -> row.get("roles", String::class.java) ?: "" }
                .awaitOneOrNull()
                ?: ""
        return serialized
            .split(",")
            .filter { it.isNotBlank() }
            .toMutableSet()
    }
}
