package br.com.locasport.identity.domain

import java.util.UUID

@JvmInline
value class StreamId(
    val value: UUID,
) {
    companion object {
        fun of(accountId: AccountId): StreamId = StreamId(accountId.value)

        fun of(credentialId: CredentialId): StreamId = StreamId(credentialId.value)
    }
}

@JvmInline
value class EventId(
    val value: UUID,
) {
    companion object {
        fun random(): EventId = EventId(UUID.randomUUID())
    }
}

@JvmInline
value class CommandId(
    val value: UUID,
)

@JvmInline
value class AccountId(
    val value: UUID,
) {
    companion object {
        fun random(): AccountId = AccountId(UUID.randomUUID())

        fun unassigned(): AccountId = AccountId(UUID(0L, 0L))
    }
}

@JvmInline
value class CredentialId(
    val value: UUID,
) {
    companion object {
        fun random(): CredentialId = CredentialId(UUID.randomUUID())

        fun unassigned(): CredentialId = CredentialId(UUID(0L, 0L))
    }
}
