package br.com.locasport.identity.application

import br.com.locasport.identity.domain.ActivateCredential
import br.com.locasport.identity.domain.CommandDeduplicationPort
import br.com.locasport.identity.domain.CompleteStepUpChallenge
import br.com.locasport.identity.domain.Credential
import br.com.locasport.identity.domain.EventPublisher
import br.com.locasport.identity.domain.EventStore
import br.com.locasport.identity.domain.ProjectionStore
import br.com.locasport.identity.domain.RegisterCredential
import br.com.locasport.identity.domain.RevokeCredential
import br.com.locasport.identity.domain.StreamId

class RegisterCredentialHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: RegisterCredential): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.credentialId)
        val history = eventStore.load(stream)
        val credential = Credential.rehydrate(history.map { it.event })
        val events = credential.register(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class ActivateCredentialHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: ActivateCredential): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.credentialId)
        val history = eventStore.load(stream)
        val credential = Credential.rehydrate(history.map { it.event })
        val events = credential.activate(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class CompleteStepUpChallengeHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: CompleteStepUpChallenge): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.credentialId)
        val history = eventStore.load(stream)
        val credential = Credential.rehydrate(history.map { it.event })
        val events = credential.completeStepUp(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}

class RevokeCredentialHandler(
    private val deduplication: CommandDeduplicationPort,
    private val eventStore: EventStore,
    private val projection: ProjectionStore,
    private val publisher: EventPublisher,
) {
    suspend fun handle(command: RevokeCredential): CommandOutcome {
        if (!deduplication.firstSeen(command.commandId)) return CommandOutcome.Discarded
        val stream = StreamId.of(command.credentialId)
        val history = eventStore.load(stream)
        val credential = Credential.rehydrate(history.map { it.event })
        val events = credential.revoke(command)
        eventStore.append(stream, history.size.toLong(), events)
        projection.apply(events)
        publisher.publish(events)
        return CommandOutcome.Applied(events.size)
    }
}
