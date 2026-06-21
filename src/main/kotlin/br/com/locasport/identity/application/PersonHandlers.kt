package br.com.locasport.identity.application

import br.com.locasport.identity.domain.ActivateAccount
import br.com.locasport.identity.domain.CommandDeduplicationPort
import br.com.locasport.identity.domain.DeactivatePerson
import br.com.locasport.identity.domain.DisclosePurpose
import br.com.locasport.identity.domain.EventPublisher
import br.com.locasport.identity.domain.EventStore
import br.com.locasport.identity.domain.ExcessiveAssuranceDenied
import br.com.locasport.identity.domain.GrantRole
import br.com.locasport.identity.domain.Person
import br.com.locasport.identity.domain.ProjectionStore
import br.com.locasport.identity.domain.RaiseAssuranceLevel
import br.com.locasport.identity.domain.ReactivatePerson
import br.com.locasport.identity.domain.RegisterPerson
import br.com.locasport.identity.domain.RevokeRole
import br.com.locasport.identity.domain.StreamId
import br.com.locasport.identity.domain.SuspendPerson

class RegisterPersonHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: RegisterPerson): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.personId)
        val history = eventStore.load(stream)
        val person = Person.rehydrate(history.map { it.event })
        val events = person.register(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class DisclosePersonPurposeHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: DisclosePurpose): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.personId)
        val history = eventStore.load(stream)
        val person = Person.rehydrate(history.map { it.event })
        val events = person.disclosePurpose(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class RaisePersonAssuranceLevelHandler(
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
        val person = Person.rehydrate(history.map { it.event })
        val events = person.raiseAssurance(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class ActivatePersonAccountHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: ActivateAccount): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.personId)
        val history = eventStore.load(stream)
        val person = Person.rehydrate(history.map { it.event })
        val events = person.activate(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class SuspendPersonHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: SuspendPerson): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.personId)
        val history = eventStore.load(stream)
        val person = Person.rehydrate(history.map { it.event })
        val events = person.suspend(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class ReactivatePersonHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: ReactivatePerson): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.personId)
        val history = eventStore.load(stream)
        val person = Person.rehydrate(history.map { it.event })
        val events = person.reactivate(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class DeactivatePersonHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: DeactivatePerson): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.personId)
        val history = eventStore.load(stream)
        val person = Person.rehydrate(history.map { it.event })
        val events = person.deactivate(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class GrantPersonRoleHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: GrantRole): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.personId)
        val history = eventStore.load(stream)
        val person = Person.rehydrate(history.map { it.event })
        val events = person.grantRole(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class RevokePersonRoleHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: RevokeRole): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.personId)
        val history = eventStore.load(stream)
        val person = Person.rehydrate(history.map { it.event })
        val events = person.revokeRole(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}
