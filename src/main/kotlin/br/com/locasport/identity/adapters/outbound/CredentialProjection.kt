package br.com.locasport.identity.adapters.outbound

import br.com.locasport.identity.domain.CredentialActivated
import br.com.locasport.identity.domain.CredentialRegistered
import br.com.locasport.identity.domain.CredentialRevoked
import br.com.locasport.identity.domain.DomainEvent
import br.com.locasport.identity.domain.ProjectionStore
import br.com.locasport.identity.domain.StepUpChallengeCompleted
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import java.time.ZoneOffset

class CredentialProjection(
    private val client: DatabaseClient,
) : ProjectionStore {
    override suspend fun apply(events: List<DomainEvent>) {
        for (event in events) {
            when (event) {
                is CredentialRegistered ->
                    client
                        .sql(
                            "INSERT INTO credential_view " +
                                "(credential_id, account_id, status, factor_type, assurance, updated_at) " +
                                "VALUES (:id, :account, 'PENDING', :factorType, 'NONE', :at) " +
                                "ON CONFLICT (credential_id) DO UPDATE SET " +
                                "account_id = EXCLUDED.account_id, status = EXCLUDED.status, " +
                                "factor_type = EXCLUDED.factor_type, updated_at = EXCLUDED.updated_at",
                        ).bind("id", event.credentialId.value)
                        .bind("account", event.accountId.value)
                        .bind("factorType", event.factorType.name)
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .await()

                is CredentialActivated ->
                    client
                        .sql("UPDATE credential_view SET status = 'ACTIVE', updated_at = :at WHERE credential_id = :id")
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.credentialId.value)
                        .await()

                is StepUpChallengeCompleted ->
                    client
                        .sql("UPDATE credential_view SET assurance = :assurance, updated_at = :at WHERE credential_id = :id")
                        .bind("assurance", event.achievedAssurance.name)
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.credentialId.value)
                        .await()

                is CredentialRevoked ->
                    client
                        .sql("UPDATE credential_view SET status = 'REVOKED', updated_at = :at WHERE credential_id = :id")
                        .bind("at", event.occurredAt.atOffset(ZoneOffset.UTC))
                        .bind("id", event.credentialId.value)
                        .await()

                else -> Unit
            }
        }
    }
}
