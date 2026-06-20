package br.com.locasport.identity.domain

data class RegisterAccount(
    val commandId: CommandId,
    val accountId: AccountId,
    val subjectType: SubjectType,
    val purposeReference: PurposeReference,
)

data class SubmitIdentityClaim(
    val commandId: CommandId,
    val accountId: AccountId,
    val claimType: ClaimType,
    val purposeReference: PurposeReference,
    val legalBasisReference: LegalBasisReference,
)

data class RaiseAssuranceLevel(
    val commandId: CommandId,
    val accountId: AccountId,
    val target: AssuranceLevel,
    val purposeReference: PurposeReference,
)

data class AssignRole(
    val commandId: CommandId,
    val accountId: AccountId,
    val role: Role,
)

data class RevokeRole(
    val commandId: CommandId,
    val accountId: AccountId,
    val role: Role,
)

data class SuspendAccount(
    val commandId: CommandId,
    val accountId: AccountId,
    val reason: String,
)

data class ReinstateAccount(
    val commandId: CommandId,
    val accountId: AccountId,
)

data class RegisterCredential(
    val commandId: CommandId,
    val credentialId: CredentialId,
    val accountId: AccountId,
    val factorType: FactorType,
)

data class ActivateCredential(
    val commandId: CommandId,
    val credentialId: CredentialId,
)

data class CompleteStepUpChallenge(
    val commandId: CommandId,
    val credentialId: CredentialId,
    val accountId: AccountId,
    val achievedAssurance: AssuranceLevel,
    val purposeReference: PurposeReference,
)

data class RevokeCredential(
    val commandId: CommandId,
    val credentialId: CredentialId,
    val reason: String,
)
