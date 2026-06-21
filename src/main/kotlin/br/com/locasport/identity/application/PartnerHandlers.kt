package br.com.locasport.identity.application

import br.com.locasport.identity.domain.CommandDeduplicationPort
import br.com.locasport.identity.domain.DeactivatePartner
import br.com.locasport.identity.domain.DisclosePartnerPurpose
import br.com.locasport.identity.domain.EventPublisher
import br.com.locasport.identity.domain.EventStore
import br.com.locasport.identity.domain.ExcessiveAssuranceDenied
import br.com.locasport.identity.domain.GrantPartnerRole
import br.com.locasport.identity.domain.Partner
import br.com.locasport.identity.domain.ProjectionStore
import br.com.locasport.identity.domain.RaiseAssuranceLevel
import br.com.locasport.identity.domain.ReactivatePartner
import br.com.locasport.identity.domain.RegisterPartner
import br.com.locasport.identity.domain.RejectPartner
import br.com.locasport.identity.domain.RevokePartnerRole
import br.com.locasport.identity.domain.StreamId
import br.com.locasport.identity.domain.SubmitPartnerForReview
import br.com.locasport.identity.domain.SuspendPartner
import br.com.locasport.identity.domain.VerifyPartnerIdentity

class RegisterPartnerHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: RegisterPartner): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.partnerId)
        val history = eventStore.load(stream)
        val partner = Partner.rehydrate(history.map { it.event })
        val events = partner.register(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class DisclosePartnerPurposeHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: DisclosePartnerPurpose): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.partnerId)
        val history = eventStore.load(stream)
        val partner = Partner.rehydrate(history.map { it.event })
        val events = partner.disclosePurpose(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class RaisePartnerAssuranceLevelHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
    private val levelUpgradePolicy: LevelUpgradePolicy,
) {
    suspend fun handle(command: RaiseAssuranceLevel): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        if (!levelUpgradePolicy.mayUpgradeTo(command.subjectId, command.target)) {
            throw ExcessiveAssuranceDenied(command.subjectId, command.target)
        }
        val stream = StreamId.of(command.subjectId)
        val history = eventStore.load(stream)
        val partner = Partner.rehydrate(history.map { it.event })
        val events = partner.raiseAssurance(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class SubmitPartnerForReviewHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: SubmitPartnerForReview): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.partnerId)
        val history = eventStore.load(stream)
        val partner = Partner.rehydrate(history.map { it.event })
        val events = partner.submitForReview(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class VerifyPartnerIdentityHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: VerifyPartnerIdentity): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.partnerId)
        val history = eventStore.load(stream)
        val partner = Partner.rehydrate(history.map { it.event })
        val events = partner.verifyIdentity(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class RejectPartnerHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: RejectPartner): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.partnerId)
        val history = eventStore.load(stream)
        val partner = Partner.rehydrate(history.map { it.event })
        val events = partner.reject(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class SuspendPartnerHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: SuspendPartner): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.partnerId)
        val history = eventStore.load(stream)
        val partner = Partner.rehydrate(history.map { it.event })
        val events = partner.suspend(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class ReactivatePartnerHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: ReactivatePartner): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.partnerId)
        val history = eventStore.load(stream)
        val partner = Partner.rehydrate(history.map { it.event })
        val events = partner.reactivate(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class DeactivatePartnerHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: DeactivatePartner): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.partnerId)
        val history = eventStore.load(stream)
        val partner = Partner.rehydrate(history.map { it.event })
        val events = partner.deactivate(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class GrantPartnerRoleHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: GrantPartnerRole): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.partnerId)
        val history = eventStore.load(stream)
        val partner = Partner.rehydrate(history.map { it.event })
        val events = partner.grantRole(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class RevokePartnerRoleHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: RevokePartnerRole): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.partnerId)
        val history = eventStore.load(stream)
        val partner = Partner.rehydrate(history.map { it.event })
        val events = partner.revokeRole(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}
