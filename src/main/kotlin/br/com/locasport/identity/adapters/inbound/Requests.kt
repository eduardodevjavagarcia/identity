package br.com.locasport.identity.adapters.inbound

import br.com.locasport.identity.domain.AccountId
import br.com.locasport.identity.domain.ActivateCredential
import br.com.locasport.identity.domain.AssignRole
import br.com.locasport.identity.domain.AssuranceLevel
import br.com.locasport.identity.domain.ClaimType
import br.com.locasport.identity.domain.CommandId
import br.com.locasport.identity.domain.CompleteStepUpChallenge
import br.com.locasport.identity.domain.CredentialId
import br.com.locasport.identity.domain.FactorType
import br.com.locasport.identity.domain.LegalBasisReference
import br.com.locasport.identity.domain.PurposeReference
import br.com.locasport.identity.domain.RaiseAssuranceLevel
import br.com.locasport.identity.domain.RegisterAccount
import br.com.locasport.identity.domain.RegisterCredential
import br.com.locasport.identity.domain.ReinstateAccount
import br.com.locasport.identity.domain.RevokeCredential
import br.com.locasport.identity.domain.RevokeRole
import br.com.locasport.identity.domain.Role
import br.com.locasport.identity.domain.SubjectType
import br.com.locasport.identity.domain.SubmitIdentityClaim
import br.com.locasport.identity.domain.SuspendAccount
import java.util.UUID

data class RegisterAccountRequest(
    val accountId: UUID,
    val subjectType: SubjectType,
    val purposeReference: String,
) {
    fun toCommand(commandId: CommandId): RegisterAccount =
        RegisterAccount(commandId, AccountId(accountId), subjectType, PurposeReference(purposeReference))
}

data class SubmitIdentityClaimRequest(
    val accountId: UUID,
    val claimType: ClaimType,
    val purposeReference: String,
    val legalBasisReference: String,
) {
    fun toCommand(commandId: CommandId): SubmitIdentityClaim =
        SubmitIdentityClaim(
            commandId,
            AccountId(accountId),
            claimType,
            PurposeReference(purposeReference),
            LegalBasisReference(legalBasisReference),
        )
}

data class RaiseAssuranceLevelRequest(
    val accountId: UUID,
    val target: AssuranceLevel,
    val purposeReference: String,
) {
    fun toCommand(commandId: CommandId): RaiseAssuranceLevel =
        RaiseAssuranceLevel(commandId, AccountId(accountId), target, PurposeReference(purposeReference))
}

data class AssignRoleRequest(
    val accountId: UUID,
    val role: Role,
) {
    fun toCommand(commandId: CommandId): AssignRole = AssignRole(commandId, AccountId(accountId), role)
}

data class RevokeRoleRequest(
    val accountId: UUID,
    val role: Role,
) {
    fun toCommand(commandId: CommandId): RevokeRole = RevokeRole(commandId, AccountId(accountId), role)
}

data class SuspendAccountRequest(
    val accountId: UUID,
    val reason: String,
) {
    fun toCommand(commandId: CommandId): SuspendAccount = SuspendAccount(commandId, AccountId(accountId), reason)
}

data class ReinstateAccountRequest(
    val accountId: UUID,
) {
    fun toCommand(commandId: CommandId): ReinstateAccount = ReinstateAccount(commandId, AccountId(accountId))
}

data class RegisterCredentialRequest(
    val credentialId: UUID,
    val accountId: UUID,
    val factorType: FactorType,
) {
    fun toCommand(commandId: CommandId): RegisterCredential =
        RegisterCredential(commandId, CredentialId(credentialId), AccountId(accountId), factorType)
}

data class ActivateCredentialRequest(
    val credentialId: UUID,
) {
    fun toCommand(commandId: CommandId): ActivateCredential = ActivateCredential(commandId, CredentialId(credentialId))
}

data class CompleteStepUpChallengeRequest(
    val credentialId: UUID,
    val accountId: UUID,
    val achievedAssurance: AssuranceLevel,
    val purposeReference: String,
) {
    fun toCommand(commandId: CommandId): CompleteStepUpChallenge =
        CompleteStepUpChallenge(
            commandId,
            CredentialId(credentialId),
            AccountId(accountId),
            achievedAssurance,
            PurposeReference(purposeReference),
        )
}

data class RevokeCredentialRequest(
    val credentialId: UUID,
    val reason: String,
) {
    fun toCommand(commandId: CommandId): RevokeCredential = RevokeCredential(commandId, CredentialId(credentialId), reason)
}
