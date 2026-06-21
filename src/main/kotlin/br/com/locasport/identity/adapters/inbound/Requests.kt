package br.com.locasport.identity.adapters.inbound

import br.com.locasport.identity.domain.ActivateAccount
import br.com.locasport.identity.domain.AssuranceLevel
import br.com.locasport.identity.domain.CommandId
import br.com.locasport.identity.domain.DeactivatePartner
import br.com.locasport.identity.domain.DeactivatePerson
import br.com.locasport.identity.domain.DisclosePartnerPurpose
import br.com.locasport.identity.domain.DisclosePurpose
import br.com.locasport.identity.domain.GrantPartnerRole
import br.com.locasport.identity.domain.GrantRole
import br.com.locasport.identity.domain.LegalBasis
import br.com.locasport.identity.domain.PartnerId
import br.com.locasport.identity.domain.PartnerType
import br.com.locasport.identity.domain.PersonId
import br.com.locasport.identity.domain.RaiseAssuranceLevel
import br.com.locasport.identity.domain.ReactivatePartner
import br.com.locasport.identity.domain.ReactivatePerson
import br.com.locasport.identity.domain.RegisterPartner
import br.com.locasport.identity.domain.RegisterPerson
import br.com.locasport.identity.domain.RejectPartner
import br.com.locasport.identity.domain.RevokePartnerRole
import br.com.locasport.identity.domain.RevokeRole
import br.com.locasport.identity.domain.Role
import br.com.locasport.identity.domain.SubmitPartnerForReview
import br.com.locasport.identity.domain.SuspendPartner
import br.com.locasport.identity.domain.SuspendPerson
import br.com.locasport.identity.domain.VerifyPartnerIdentity
import java.time.Instant
import java.util.UUID

data class RegisterPersonRequest(
    val personId: UUID,
    val legalBasis: LegalBasis,
) {
    fun toCommand(commandId: CommandId): RegisterPerson = RegisterPerson(commandId, PersonId(personId), legalBasis)
}

data class ActivateAccountRequest(
    val personId: UUID,
) {
    fun toCommand(commandId: CommandId): ActivateAccount = ActivateAccount(commandId, PersonId(personId))
}

data class SuspendPersonRequest(
    val personId: UUID,
    val reason: String,
) {
    fun toCommand(commandId: CommandId): SuspendPerson = SuspendPerson(commandId, PersonId(personId), reason)
}

data class ReactivatePersonRequest(
    val personId: UUID,
) {
    fun toCommand(commandId: CommandId): ReactivatePerson = ReactivatePerson(commandId, PersonId(personId))
}

data class DeactivatePersonRequest(
    val personId: UUID,
) {
    fun toCommand(commandId: CommandId): DeactivatePerson = DeactivatePerson(commandId, PersonId(personId))
}

data class DisclosePurposeRequest(
    val personId: UUID,
    val purpose: String,
) {
    fun toCommand(commandId: CommandId): DisclosePurpose = DisclosePurpose(commandId, PersonId(personId), purpose)
}

data class RaiseAssuranceLevelRequest(
    val subjectId: UUID,
    val target: AssuranceLevel,
) {
    fun toCommand(commandId: CommandId): RaiseAssuranceLevel = RaiseAssuranceLevel(commandId, subjectId, target)
}

data class GrantRoleRequest(
    val personId: UUID,
    val role: Role,
) {
    fun toCommand(commandId: CommandId): GrantRole = GrantRole(commandId, PersonId(personId), role)
}

data class RevokeRoleRequest(
    val personId: UUID,
    val role: Role,
) {
    fun toCommand(commandId: CommandId): RevokeRole = RevokeRole(commandId, PersonId(personId), role)
}

data class RegisterPartnerRequest(
    val partnerId: UUID,
    val partnerType: PartnerType,
    val legalName: String,
    val taxId: String,
    val legalBasis: LegalBasis,
) {
    fun toCommand(commandId: CommandId): RegisterPartner =
        RegisterPartner(commandId, PartnerId(partnerId), partnerType, legalName, taxId, legalBasis)
}

data class SubmitPartnerForReviewRequest(
    val partnerId: UUID,
) {
    fun toCommand(commandId: CommandId): SubmitPartnerForReview = SubmitPartnerForReview(commandId, PartnerId(partnerId))
}

data class VerifyPartnerIdentityRequest(
    val partnerId: UUID,
) {
    fun toCommand(commandId: CommandId): VerifyPartnerIdentity = VerifyPartnerIdentity(commandId, PartnerId(partnerId), Instant.now())
}

data class RejectPartnerRequest(
    val partnerId: UUID,
    val reason: String,
) {
    fun toCommand(commandId: CommandId): RejectPartner = RejectPartner(commandId, PartnerId(partnerId), reason)
}

data class SuspendPartnerRequest(
    val partnerId: UUID,
    val reason: String,
) {
    fun toCommand(commandId: CommandId): SuspendPartner = SuspendPartner(commandId, PartnerId(partnerId), reason)
}

data class ReactivatePartnerRequest(
    val partnerId: UUID,
) {
    fun toCommand(commandId: CommandId): ReactivatePartner = ReactivatePartner(commandId, PartnerId(partnerId))
}

data class DeactivatePartnerRequest(
    val partnerId: UUID,
) {
    fun toCommand(commandId: CommandId): DeactivatePartner = DeactivatePartner(commandId, PartnerId(partnerId))
}

data class DisclosePartnerPurposeRequest(
    val partnerId: UUID,
    val purpose: String,
) {
    fun toCommand(commandId: CommandId): DisclosePartnerPurpose = DisclosePartnerPurpose(commandId, PartnerId(partnerId), purpose)
}

data class GrantPartnerRoleRequest(
    val partnerId: UUID,
    val role: Role,
) {
    fun toCommand(commandId: CommandId): GrantPartnerRole = GrantPartnerRole(commandId, PartnerId(partnerId), role)
}

data class RevokePartnerRoleRequest(
    val partnerId: UUID,
    val role: Role,
) {
    fun toCommand(commandId: CommandId): RevokePartnerRole = RevokePartnerRole(commandId, PartnerId(partnerId), role)
}
