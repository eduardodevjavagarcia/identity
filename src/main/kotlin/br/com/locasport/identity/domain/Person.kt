package br.com.locasport.identity.domain

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Person private constructor(
    private var id: PersonId,
    private var status: PersonStatus,
    private var assurance: AssuranceLevel,
    private var legalBasis: LegalBasis,
    private var purposeDisclosed: Boolean,
    private val roles: MutableSet<Role>,
) {
    private val mutex = Mutex()

    suspend fun register(command: RegisterPerson): List<DomainEvent> =
        mutex.withLock {
            ensure(id == PersonId.unassigned()) { InvalidStateTransition(status, PersonStatus.REGISTERED) }
            id = command.personId
            legalBasis = command.legalBasis
            listOf(PersonRegistered(command.personId, command.legalBasis))
        }

    suspend fun disclosePurpose(command: DisclosePurpose): List<DomainEvent> =
        mutex.withLock {
            ensure(status != PersonStatus.DEACTIVATED && status != PersonStatus.SUSPENDED) {
                InvalidStateTransition(status, status)
            }
            if (purposeDisclosed) return@withLock emptyList()
            purposeDisclosed = true
            listOf(PurposeDisclosed(id.value, command.purpose))
        }

    suspend fun raiseAssurance(command: RaiseAssuranceLevel): List<DomainEvent> =
        mutex.withLock {
            ensure(status == PersonStatus.REGISTERED || status == PersonStatus.ACTIVE) {
                InvalidStateTransition(status, status)
            }
            ensure(purposeDisclosed) { PurposeNotDisclosed(id) }
            if (command.target <= assurance) throw InvalidAssuranceDowngrade(assurance, command.target)
            val previous = assurance
            assurance = command.target
            listOf(AssuranceLevelRaised(id.value, previous, command.target))
        }

    suspend fun activate(command: ActivateAccount): List<DomainEvent> =
        mutex.withLock {
            ensure(status == PersonStatus.REGISTERED) { InvalidStateTransition(status, PersonStatus.ACTIVE) }
            ensure(assurance >= AssuranceLevel.BASIC) { AccountActivationDenied(id, assurance) }
            status = PersonStatus.ACTIVE
            listOf(PersonActivated(id))
        }

    suspend fun suspend(command: SuspendPerson): List<DomainEvent> =
        mutex.withLock {
            ensure(status == PersonStatus.ACTIVE) { InvalidStateTransition(status, PersonStatus.SUSPENDED) }
            status = PersonStatus.SUSPENDED
            listOf(PersonSuspended(id, command.reason))
        }

    suspend fun reactivate(command: ReactivatePerson): List<DomainEvent> =
        mutex.withLock {
            ensure(status == PersonStatus.SUSPENDED) { InvalidStateTransition(status, PersonStatus.ACTIVE) }
            status = PersonStatus.ACTIVE
            listOf(PersonReactivated(id))
        }

    suspend fun deactivate(command: DeactivatePerson): List<DomainEvent> =
        mutex.withLock {
            ensure(status == PersonStatus.ACTIVE || status == PersonStatus.SUSPENDED) {
                InvalidStateTransition(status, PersonStatus.DEACTIVATED)
            }
            status = PersonStatus.DEACTIVATED
            listOf(PersonDeactivated(id))
        }

    suspend fun grantRole(command: GrantRole): List<DomainEvent> =
        mutex.withLock {
            ensure(status == PersonStatus.ACTIVE) { InvalidStateTransition(status, status) }
            if (!roles.add(command.role)) return@withLock emptyList()
            listOf(RoleGranted(id.value, command.role))
        }

    suspend fun revokeRole(command: RevokeRole): List<DomainEvent> =
        mutex.withLock {
            if (!roles.remove(command.role)) return@withLock emptyList()
            listOf(RoleRevoked(id.value, command.role))
        }

    private fun apply(event: DomainEvent): Person {
        when (event) {
            is PersonRegistered -> {
                id = event.personId
                legalBasis = event.legalBasis
                status = PersonStatus.REGISTERED
            }
            is PurposeDisclosed -> purposeDisclosed = true
            is AssuranceLevelRaised -> assurance = event.to
            is PersonActivated -> status = PersonStatus.ACTIVE
            is PersonSuspended -> status = PersonStatus.SUSPENDED
            is PersonReactivated -> status = PersonStatus.ACTIVE
            is PersonDeactivated -> status = PersonStatus.DEACTIVATED
            is RoleGranted -> roles.add(event.role)
            is RoleRevoked -> roles.remove(event.role)
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
        fun rehydrate(history: List<DomainEvent>): Person = history.fold(empty()) { state, event -> state.apply(event) }

        private fun empty(): Person =
            Person(
                PersonId.unassigned(),
                PersonStatus.REGISTERED,
                AssuranceLevel.NONE,
                LegalBasis.CONTRACT_EXECUTION,
                false,
                mutableSetOf(),
            )
    }
}
