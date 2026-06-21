package br.com.locasport.identity.domain

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Partner private constructor(
    private var id: PartnerId,
    private var status: PartnerStatus,
    private var assurance: AssuranceLevel,
    private var partnerType: PartnerType,
    private var legalName: String,
    private var taxId: String,
    private var legalBasis: LegalBasis,
    private var purposeDisclosed: Boolean,
    private val roles: MutableSet<Role>,
) {
    private val mutex = Mutex()

    suspend fun register(command: RegisterPartner): List<DomainEvent> =
        mutex.withLock {
            ensure(id == PartnerId.unassigned()) { InvalidStateTransition(status, PartnerStatus.REGISTERED) }
            id = command.partnerId
            partnerType = command.partnerType
            legalName = command.legalName
            taxId = command.taxId
            legalBasis = command.legalBasis
            listOf(PartnerRegistered(command.partnerId, command.partnerType, command.legalName, command.taxId, command.legalBasis))
        }

    suspend fun disclosePurpose(command: DisclosePartnerPurpose): List<DomainEvent> =
        mutex.withLock {
            ensure(status != PartnerStatus.DEACTIVATED && status != PartnerStatus.REJECTED && status != PartnerStatus.SUSPENDED) {
                InvalidStateTransition(status, status)
            }
            if (purposeDisclosed) return@withLock emptyList()
            purposeDisclosed = true
            listOf(PurposeDisclosed(id.value, command.purpose))
        }

    suspend fun raiseAssurance(command: RaiseAssuranceLevel): List<DomainEvent> =
        mutex.withLock {
            ensure(
                status == PartnerStatus.REGISTERED ||
                    status == PartnerStatus.UNDER_REVIEW ||
                    status == PartnerStatus.ACTIVE,
            ) { InvalidStateTransition(status, status) }
            ensure(purposeDisclosed) { PurposeNotDisclosed(id) }
            if (command.target <= assurance) throw InvalidAssuranceDowngrade(assurance, command.target)
            val previous = assurance
            assurance = command.target
            listOf(AssuranceLevelRaised(id.value, previous, command.target))
        }

    suspend fun submitForReview(command: SubmitPartnerForReview): List<DomainEvent> =
        mutex.withLock {
            ensure(status == PartnerStatus.REGISTERED) { InvalidStateTransition(status, PartnerStatus.UNDER_REVIEW) }
            status = PartnerStatus.UNDER_REVIEW
            listOf(PartnerReviewSubmitted(id))
        }

    suspend fun verifyIdentity(command: VerifyPartnerIdentity): List<DomainEvent> =
        mutex.withLock {
            ensure(purposeDisclosed) { PurposeNotDisclosed(id) }
            ensure(status == PartnerStatus.UNDER_REVIEW) { InvalidStateTransition(status, PartnerStatus.ACTIVE) }
            ensure(assurance == AssuranceLevel.VERIFIED) { PartnerApprovalDenied(id, assurance) }
            status = PartnerStatus.ACTIVE
            listOf(
                PartnerIdentityVerified(id, legalName, taxId, partnerType, assurance, command.verifiedAt),
            )
        }

    suspend fun reject(command: RejectPartner): List<DomainEvent> =
        mutex.withLock {
            ensure(status == PartnerStatus.UNDER_REVIEW) { InvalidStateTransition(status, PartnerStatus.REJECTED) }
            status = PartnerStatus.REJECTED
            listOf(PartnerRejected(id, command.reason))
        }

    suspend fun suspend(command: SuspendPartner): List<DomainEvent> =
        mutex.withLock {
            ensure(status == PartnerStatus.ACTIVE) { InvalidStateTransition(status, PartnerStatus.SUSPENDED) }
            status = PartnerStatus.SUSPENDED
            listOf(PartnerSuspended(id, command.reason))
        }

    suspend fun reactivate(command: ReactivatePartner): List<DomainEvent> =
        mutex.withLock {
            ensure(status == PartnerStatus.SUSPENDED) { InvalidStateTransition(status, PartnerStatus.ACTIVE) }
            status = PartnerStatus.ACTIVE
            listOf(PartnerReactivated(id))
        }

    suspend fun deactivate(command: DeactivatePartner): List<DomainEvent> =
        mutex.withLock {
            ensure(status == PartnerStatus.ACTIVE || status == PartnerStatus.SUSPENDED) {
                InvalidStateTransition(status, PartnerStatus.DEACTIVATED)
            }
            status = PartnerStatus.DEACTIVATED
            listOf(PartnerDeactivated(id))
        }

    suspend fun grantRole(command: GrantPartnerRole): List<DomainEvent> =
        mutex.withLock {
            ensure(status == PartnerStatus.ACTIVE) { InvalidStateTransition(status, status) }
            if (!roles.add(command.role)) return@withLock emptyList()
            listOf(RoleGranted(id.value, command.role))
        }

    suspend fun revokeRole(command: RevokePartnerRole): List<DomainEvent> =
        mutex.withLock {
            if (!roles.remove(command.role)) return@withLock emptyList()
            listOf(RoleRevoked(id.value, command.role))
        }

    private fun apply(event: DomainEvent): Partner {
        when (event) {
            is PartnerRegistered -> {
                id = event.partnerId
                partnerType = event.partnerType
                legalName = event.legalName
                taxId = event.taxId
                legalBasis = event.legalBasis
                status = PartnerStatus.REGISTERED
            }
            is PurposeDisclosed -> purposeDisclosed = true
            is AssuranceLevelRaised -> assurance = event.to
            is PartnerReviewSubmitted -> status = PartnerStatus.UNDER_REVIEW
            is PartnerIdentityVerified -> {
                status = PartnerStatus.ACTIVE
                assurance = AssuranceLevel.VERIFIED
            }
            is PartnerRejected -> status = PartnerStatus.REJECTED
            is PartnerSuspended -> status = PartnerStatus.SUSPENDED
            is PartnerReactivated -> status = PartnerStatus.ACTIVE
            is PartnerDeactivated -> status = PartnerStatus.DEACTIVATED
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
        fun rehydrate(history: List<DomainEvent>): Partner = history.fold(empty()) { state, event -> state.apply(event) }

        private fun empty(): Partner =
            Partner(
                PartnerId.unassigned(),
                PartnerStatus.REGISTERED,
                AssuranceLevel.NONE,
                PartnerType.ARENA,
                "",
                "",
                LegalBasis.CONTRACT_EXECUTION,
                false,
                mutableSetOf(),
            )
    }
}
