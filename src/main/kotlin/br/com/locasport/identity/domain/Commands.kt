package br.com.locasport.identity.domain

import java.time.Instant

data class RegisterPerson(
    val commandId: CommandId,
    val personId: PersonId,
    val legalBasis: LegalBasis,
)

data class ActivateAccount(
    val commandId: CommandId,
    val personId: PersonId,
)

data class SuspendPerson(
    val commandId: CommandId,
    val personId: PersonId,
    val reason: String,
)

data class ReactivatePerson(
    val commandId: CommandId,
    val personId: PersonId,
)

data class DeactivatePerson(
    val commandId: CommandId,
    val personId: PersonId,
)

data class DisclosePurpose(
    val commandId: CommandId,
    val personId: PersonId,
    val purpose: String,
)

data class RaiseAssuranceLevel(
    val commandId: CommandId,
    val subjectId: java.util.UUID,
    val target: AssuranceLevel,
)

data class GrantRole(
    val commandId: CommandId,
    val personId: PersonId,
    val role: Role,
)

data class RevokeRole(
    val commandId: CommandId,
    val personId: PersonId,
    val role: Role,
)

data class RegisterPartner(
    val commandId: CommandId,
    val partnerId: PartnerId,
    val partnerType: PartnerType,
    val legalName: String,
    val taxId: String,
    val legalBasis: LegalBasis,
)

data class SubmitPartnerForReview(
    val commandId: CommandId,
    val partnerId: PartnerId,
)

data class VerifyPartnerIdentity(
    val commandId: CommandId,
    val partnerId: PartnerId,
    val verifiedAt: Instant,
)

data class RejectPartner(
    val commandId: CommandId,
    val partnerId: PartnerId,
    val reason: String,
)

data class SuspendPartner(
    val commandId: CommandId,
    val partnerId: PartnerId,
    val reason: String,
)

data class ReactivatePartner(
    val commandId: CommandId,
    val partnerId: PartnerId,
)

data class DeactivatePartner(
    val commandId: CommandId,
    val partnerId: PartnerId,
)

data class DisclosePartnerPurpose(
    val commandId: CommandId,
    val partnerId: PartnerId,
    val purpose: String,
)

data class GrantPartnerRole(
    val commandId: CommandId,
    val partnerId: PartnerId,
    val role: Role,
)

data class RevokePartnerRole(
    val commandId: CommandId,
    val partnerId: PartnerId,
    val role: Role,
)
