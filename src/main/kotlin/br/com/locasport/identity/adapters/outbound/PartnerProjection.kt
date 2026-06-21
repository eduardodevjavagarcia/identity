package br.com.locasport.identity.adapters.outbound

import br.com.locasport.identity.domain.DomainEvent
import br.com.locasport.identity.domain.PartnerDeactivated
import br.com.locasport.identity.domain.PartnerIdentityVerified
import br.com.locasport.identity.domain.PartnerReactivated
import br.com.locasport.identity.domain.PartnerRegistered
import br.com.locasport.identity.domain.PartnerRejected
import br.com.locasport.identity.domain.PartnerReviewSubmitted
import br.com.locasport.identity.domain.PartnerSuspended
import br.com.locasport.identity.domain.ProjectionStore
import br.com.locasport.identity.domain.PurposeDisclosed
import br.com.locasport.identity.domain.RoleGranted
import br.com.locasport.identity.domain.RoleRevoked
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import org.springframework.r2dbc.core.awaitOneOrNull
import java.time.ZoneOffset

class PartnerProjection(
    private val client: DatabaseClient,
) : ProjectionStore {
    override suspend fun apply(events: List<DomainEvent>) {
        for (event in events) {
            when (event) {
                is PartnerRegistered ->
                    client
                        .sql(
                            "INSERT INTO partner_view " +
                                "(partner_id, partner_type, legal_name, tax_id, " +
                                "status, assurance, roles, legal_basis, " +
                                "purpose_disclosed, updated_at) " +
                                "VALUES (:id, :partnerType, :legalName, :taxId, " +
                                "'REGISTERED', 'NONE', '', :legalBasis, FALSE, :at) " +
                                "ON CONFLICT (partner_id) DO UPDATE SET " +
                                "partner_type = EXCLUDED.partner_type, " +
                                "legal_name = EXCLUDED.legal_name, " +
                                "tax_id = EXCLUDED.tax_id, " +
                                "legal_basis = EXCLUDED.legal_basis, " +
                                "status = EXCLUDED.status, " +
                                "updated_at = EXCLUDED.updated_at",
                        ).bind("id", event.partnerId.value)
                        .bind("partnerType", event.partnerType.name)
                        .bind("legalName", event.legalName)
                        .bind("taxId", event.taxId)
                        .bind("legalBasis", event.legalBasis.name)
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .await()

                is PartnerReviewSubmitted ->
                    client
                        .sql("UPDATE partner_view SET status = 'UNDER_REVIEW', updated_at = :at WHERE partner_id = :id")
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.partnerId.value)
                        .await()

                is PartnerIdentityVerified ->
                    client
                        .sql(
                            "UPDATE partner_view SET status = 'ACTIVE', assurance = :assurance, " +
                                "updated_at = :at WHERE partner_id = :id",
                        ).bind("assurance", event.assuranceLevel.name)
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.partnerId.value)
                        .await()

                is PartnerRejected ->
                    client
                        .sql("UPDATE partner_view SET status = 'REJECTED', updated_at = :at WHERE partner_id = :id")
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.partnerId.value)
                        .await()

                is PartnerSuspended ->
                    client
                        .sql("UPDATE partner_view SET status = 'SUSPENDED', updated_at = :at WHERE partner_id = :id")
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.partnerId.value)
                        .await()

                is PartnerReactivated ->
                    client
                        .sql("UPDATE partner_view SET status = 'ACTIVE', updated_at = :at WHERE partner_id = :id")
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.partnerId.value)
                        .await()

                is PartnerDeactivated ->
                    client
                        .sql("UPDATE partner_view SET status = 'DEACTIVATED', updated_at = :at WHERE partner_id = :id")
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.partnerId.value)
                        .await()

                is PurposeDisclosed ->
                    client
                        .sql(
                            "UPDATE partner_view SET purpose_disclosed = TRUE, " +
                                "updated_at = :at WHERE partner_id = :id",
                        ).bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.subjectId)
                        .await()

                is RoleGranted -> {
                    val roles = currentRoles(event.subjectId)
                    roles.add(event.role.name)
                    client
                        .sql("UPDATE partner_view SET roles = :roles, updated_at = :at WHERE partner_id = :id")
                        .bind("roles", roles.sorted().joinToString(","))
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.subjectId)
                        .await()
                }

                is RoleRevoked -> {
                    val roles = currentRoles(event.subjectId)
                    roles.remove(event.role.name)
                    client
                        .sql("UPDATE partner_view SET roles = :roles, updated_at = :at WHERE partner_id = :id")
                        .bind("roles", roles.sorted().joinToString(","))
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.subjectId)
                        .await()
                }

                else -> Unit
            }
        }
    }

    private suspend fun currentRoles(partnerId: java.util.UUID): MutableSet<String> {
        val serialized =
            client
                .sql("SELECT roles FROM partner_view WHERE partner_id = :id")
                .bind("id", partnerId)
                .map { row -> row.get("roles", String::class.java) ?: "" }
                .awaitOneOrNull()
                ?: ""
        return serialized
            .split(",")
            .filter { it.isNotBlank() }
            .toMutableSet()
    }
}
