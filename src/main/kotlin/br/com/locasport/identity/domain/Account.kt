package br.com.locasport.identity.domain

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class AccountStatus {
    REGISTERED,
    IDENTIFIED,
    VERIFIED,
    SUSPENDED,
}

class Account private constructor(
    private var id: AccountId,
    private var status: AccountStatus,
    private var subjectType: SubjectType,
    private var assurance: AssuranceLevel,
    private val roles: MutableSet<Role>,
) {
    private val mutex = Mutex()

    suspend fun register(command: RegisterAccount): List<DomainEvent> =
        mutex.withLock {
            ensure(id == AccountId.unassigned()) { InvalidStateTransition(status, AccountStatus.REGISTERED) }
            id = command.accountId
            subjectType = command.subjectType
            listOf(AccountRegistered(command.accountId, command.subjectType, command.purposeReference))
        }

    suspend fun submitClaim(command: SubmitIdentityClaim): List<DomainEvent> =
        mutex.withLock {
            ensure(status != AccountStatus.SUSPENDED) { InvalidStateTransition(status, status) }
            listOf(IdentityClaimSubmitted(id, command.claimType, command.purposeReference, command.legalBasisReference))
        }

    suspend fun raiseAssurance(command: RaiseAssuranceLevel): List<DomainEvent> =
        mutex.withLock {
            ensure(status != AccountStatus.SUSPENDED) { InvalidStateTransition(status, status) }
            if (command.target <= assurance) throw InvalidAssuranceDowngrade(assurance, command.target)
            val next = promote(status, command.target)
            ensure(!skipsIdentification(status, next)) { InvalidStateTransition(status, next) }
            val previous = assurance
            assurance = command.target
            status = next
            listOf(AssuranceLevelRaised(id, previous, command.target, command.purposeReference))
        }

    suspend fun assignRole(command: AssignRole): List<DomainEvent> =
        mutex.withLock {
            ensure(status != AccountStatus.SUSPENDED) { InvalidStateTransition(status, status) }
            if (!roles.add(command.role)) return@withLock emptyList()
            listOf(RoleAssigned(id, command.role))
        }

    suspend fun revokeRole(command: RevokeRole): List<DomainEvent> =
        mutex.withLock {
            if (!roles.remove(command.role)) return@withLock emptyList()
            listOf(RoleRevoked(id, command.role))
        }

    suspend fun suspendAccount(command: SuspendAccount): List<DomainEvent> =
        mutex.withLock {
            ensure(status != AccountStatus.SUSPENDED) { InvalidStateTransition(status, AccountStatus.SUSPENDED) }
            status = AccountStatus.SUSPENDED
            listOf(AccountSuspended(id, command.reason))
        }

    suspend fun reinstate(command: ReinstateAccount): List<DomainEvent> =
        mutex.withLock {
            ensure(status == AccountStatus.SUSPENDED) { InvalidStateTransition(status, AccountStatus.IDENTIFIED) }
            status = restored()
            listOf(AccountReinstated(id))
        }

    private fun promote(
        current: AccountStatus,
        target: AssuranceLevel,
    ): AccountStatus =
        when {
            target >= AssuranceLevel.HIGH -> AccountStatus.VERIFIED
            target >= AssuranceLevel.SUBSTANTIAL -> AccountStatus.IDENTIFIED
            else -> current
        }

    private fun restored(): AccountStatus =
        if (assurance >= AssuranceLevel.SUBSTANTIAL) AccountStatus.IDENTIFIED else AccountStatus.REGISTERED

    private fun skipsIdentification(
        current: AccountStatus,
        next: AccountStatus,
    ): Boolean = current == AccountStatus.REGISTERED && next == AccountStatus.VERIFIED

    private fun apply(event: DomainEvent): Account {
        when (event) {
            is AccountRegistered -> {
                id = event.accountId
                subjectType = event.subjectType
                status = AccountStatus.REGISTERED
            }
            is AssuranceLevelRaised -> {
                assurance = event.to
                status = promote(status, event.to)
            }
            is RoleAssigned -> roles.add(event.role)
            is RoleRevoked -> roles.remove(event.role)
            is AccountSuspended -> status = AccountStatus.SUSPENDED
            is AccountReinstated -> status = restored()
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
        fun rehydrate(history: List<DomainEvent>): Account = history.fold(empty()) { state, event -> state.apply(event) }

        private fun empty(): Account =
            Account(AccountId.unassigned(), AccountStatus.REGISTERED, SubjectType.PERSON, AssuranceLevel.NONE, mutableSetOf())
    }
}
