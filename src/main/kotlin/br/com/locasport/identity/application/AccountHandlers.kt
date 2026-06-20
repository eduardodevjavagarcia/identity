package br.com.locasport.identity.application

import br.com.locasport.identity.domain.Account
import br.com.locasport.identity.domain.AssignRole
import br.com.locasport.identity.domain.CommandDeduplicationPort
import br.com.locasport.identity.domain.EventPublisher
import br.com.locasport.identity.domain.EventStore
import br.com.locasport.identity.domain.ProjectionStore
import br.com.locasport.identity.domain.RaiseAssuranceLevel
import br.com.locasport.identity.domain.RegisterAccount
import br.com.locasport.identity.domain.ReinstateAccount
import br.com.locasport.identity.domain.RevokeRole
import br.com.locasport.identity.domain.StreamId
import br.com.locasport.identity.domain.SubmitIdentityClaim
import br.com.locasport.identity.domain.SuspendAccount

class RegisterAccountHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: RegisterAccount): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.accountId)
        val history = eventStore.load(stream)
        val account = Account.rehydrate(history.map { it.event })
        val events = account.register(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class SubmitIdentityClaimHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: SubmitIdentityClaim): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.accountId)
        val history = eventStore.load(stream)
        val account = Account.rehydrate(history.map { it.event })
        val events = account.submitClaim(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class RaiseAssuranceLevelHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: RaiseAssuranceLevel): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.accountId)
        val history = eventStore.load(stream)
        val account = Account.rehydrate(history.map { it.event })
        val events = account.raiseAssurance(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class AssignRoleHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: AssignRole): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.accountId)
        val history = eventStore.load(stream)
        val account = Account.rehydrate(history.map { it.event })
        val events = account.assignRole(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class SuspendAccountHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: SuspendAccount): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.accountId)
        val history = eventStore.load(stream)
        val account = Account.rehydrate(history.map { it.event })
        val events = account.suspendAccount(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class RevokeRoleHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: RevokeRole): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.accountId)
        val history = eventStore.load(stream)
        val account = Account.rehydrate(history.map { it.event })
        val events = account.revokeRole(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class ReinstateAccountHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: ReinstateAccount): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.accountId)
        val history = eventStore.load(stream)
        val account = Account.rehydrate(history.map { it.event })
        val events = account.reinstate(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}
