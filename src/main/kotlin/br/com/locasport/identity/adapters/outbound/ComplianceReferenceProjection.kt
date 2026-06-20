package br.com.locasport.identity.adapters.outbound

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

data class ComplianceReference(
    val referenceId: UUID,
    val sourceType: String,
    val purposeReference: String,
    val legalBasisReference: String,
)

class ComplianceReferenceProjection(
    private val client: DatabaseClient,
) {
    suspend fun upsert(reference: ComplianceReference) {
        client
            .sql(
                "INSERT INTO compliance_reference_view " +
                    "(reference_id, source_type, purpose_reference, legal_basis_reference, updated_at) " +
                    "VALUES (:referenceId, :sourceType, :purposeReference, :legalBasisReference, :updatedAt) " +
                    "ON CONFLICT (reference_id) DO UPDATE SET " +
                    "source_type = EXCLUDED.source_type, purpose_reference = EXCLUDED.purpose_reference, " +
                    "legal_basis_reference = EXCLUDED.legal_basis_reference, updated_at = EXCLUDED.updated_at",
            ).bind("referenceId", reference.referenceId)
            .bind("sourceType", reference.sourceType)
            .bind("purposeReference", reference.purposeReference)
            .bind("legalBasisReference", reference.legalBasisReference)
            .bind("updatedAt", OffsetDateTime.now(ZoneOffset.UTC))
            .await()
    }
}
