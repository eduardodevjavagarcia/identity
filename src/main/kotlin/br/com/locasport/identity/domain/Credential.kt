package br.com.locasport.identity.domain

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class CredentialStatus {
    PENDING,
    ACTIVE,
    REVOKED,
}

class Credential private constructor(
    private var id: CredentialId,
    private var accountId: AccountId,
    private var status: CredentialStatus,
    private var factorType: FactorType,
    private var assurance: AssuranceLevel,
) {
    private val mutex = Mutex()

    suspend fun register(command: RegisterCredential): List<DomainEvent> =
        mutex.withLock {
            ensure(id == CredentialId.unassigned()) { InvalidStateTransition(status, CredentialStatus.PENDING) }
            id = command.credentialId
            accountId = command.accountId
            factorType = command.factorType
            listOf(CredentialRegistered(command.credentialId, command.accountId, command.factorType))
        }

    suspend fun activate(command: ActivateCredential): List<DomainEvent> =
        mutex.withLock {
            ensure(status == CredentialStatus.PENDING) { InvalidStateTransition(status, CredentialStatus.ACTIVE) }
            status = CredentialStatus.ACTIVE
            listOf(CredentialActivated(id))
        }

    suspend fun completeStepUp(command: CompleteStepUpChallenge): List<DomainEvent> =
        mutex.withLock {
            ensure(status == CredentialStatus.ACTIVE) { InvalidStateTransition(status, CredentialStatus.ACTIVE) }
            assurance = command.achievedAssurance
            listOf(StepUpChallengeCompleted(id, accountId, command.achievedAssurance, command.purposeReference))
        }

    suspend fun revoke(command: RevokeCredential): List<DomainEvent> =
        mutex.withLock {
            ensure(status == CredentialStatus.ACTIVE) { InvalidStateTransition(status, CredentialStatus.REVOKED) }
            status = CredentialStatus.REVOKED
            listOf(CredentialRevoked(id, command.reason))
        }

    private fun apply(event: DomainEvent): Credential {
        when (event) {
            is CredentialRegistered -> {
                id = event.credentialId
                accountId = event.accountId
                factorType = event.factorType
                status = CredentialStatus.PENDING
            }
            is CredentialActivated -> status = CredentialStatus.ACTIVE
            is StepUpChallengeCompleted -> assurance = event.achievedAssurance
            is CredentialRevoked -> status = CredentialStatus.REVOKED
            else -> Unit
        }
        return this
    }

    private inline fun ensure(
        condition: Boolean,
        error: () -> RuntimeException,
    ) {
        if (!condition) throw error()
    }

    companion object {
        fun rehydrate(history: List<DomainEvent>): Credential = history.fold(empty()) { state, event -> state.apply(event) }

        private fun empty(): Credential =
            Credential(
                CredentialId.unassigned(),
                AccountId.unassigned(),
                CredentialStatus.PENDING,
                FactorType.PASSWORD,
                AssuranceLevel.NONE,
            )
    }
}
