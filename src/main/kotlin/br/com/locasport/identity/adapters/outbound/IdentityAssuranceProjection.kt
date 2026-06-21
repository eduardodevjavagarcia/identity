package br.com.locasport.identity.adapters.outbound

import br.com.locasport.identity.domain.AssuranceLevelRaised
import br.com.locasport.identity.domain.DomainEvent
import br.com.locasport.identity.domain.PartnerIdentityVerified
import br.com.locasport.identity.domain.PartnerRegistered
import br.com.locasport.identity.domain.PersonRegistered
import br.com.locasport.identity.domain.ProjectionStore
import br.com.locasport.identity.domain.PurposeDisclosed
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import java.time.ZoneOffset

class IdentityAssuranceProjection(
    private val client: DatabaseClient,
) : ProjectionStore {
    override suspend fun apply(events: List<DomainEvent>) {
        for (event in events) {
            when (event) {
                is PersonRegistered ->
                    client
                        .sql(
                            "INSERT INTO identity_assurance_view " +
                                "(subject_id, subject_type, assurance, purpose_disclosed, updated_at) " +
                                "VALUES (:id, 'PERSON', 'NONE', FALSE, :at) " +
                                "ON CONFLICT (subject_id) DO NOTHING",
                        ).bind("id", event.personId.value)
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .await()

                is PartnerRegistered ->
                    client
                        .sql(
                            "INSERT INTO identity_assurance_view " +
                                "(subject_id, subject_type, assurance, purpose_disclosed, updated_at) " +
                                "VALUES (:id, 'PARTNER', 'NONE', FALSE, :at) " +
                                "ON CONFLICT (subject_id) DO NOTHING",
                        ).bind("id", event.partnerId.value)
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .await()

                is AssuranceLevelRaised ->
                    client
                        .sql(
                            "UPDATE identity_assurance_view SET assurance = :assurance, " +
                                "updated_at = :at WHERE subject_id = :id",
                        ).bind("assurance", event.to.name)
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.subjectId)
                        .await()

                is PartnerIdentityVerified ->
                    client
                        .sql(
                            "UPDATE identity_assurance_view SET assurance = :assurance, " +
                                "updated_at = :at WHERE subject_id = :id",
                        ).bind("assurance", event.assuranceLevel.name)
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.partnerId.value)
                        .await()

                is PurposeDisclosed ->
                    client
                        .sql(
                            "UPDATE identity_assurance_view SET purpose_disclosed = TRUE, " +
                                "updated_at = :at WHERE subject_id = :id",
                        ).bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.subjectId)
                        .await()

                else -> Unit
            }
        }
    }
}
