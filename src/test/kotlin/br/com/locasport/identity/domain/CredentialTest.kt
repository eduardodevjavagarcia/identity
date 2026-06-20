package br.com.locasport.identity.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import java.util.UUID

class CredentialTest :
    FunSpec({
        val credentialId = CredentialId.random()
        val accountId = AccountId.random()
        val purpose = PurposeReference("p")

        fun command() = CommandId(UUID.randomUUID())

        test("registra credencial emite CredentialRegistered") {
            runTest {
                val credential = Credential.rehydrate(emptyList())
                credential
                    .register(RegisterCredential(command(), credentialId, accountId, FactorType.PASSWORD))
                    .single()
                    .shouldBeInstanceOf<CredentialRegistered>()
            }
        }

        test("ativa credencial PENDING para ACTIVE") {
            runTest {
                val credential =
                    Credential.rehydrate(
                        listOf(CredentialRegistered(credentialId, accountId, FactorType.PASSWORD)),
                    )
                credential
                    .activate(ActivateCredential(command(), credentialId))
                    .single()
                    .shouldBeInstanceOf<CredentialActivated>()
            }
        }

        test("rejeita ativar credencial ja ACTIVE") {
            runTest {
                val credential =
                    Credential.rehydrate(
                        listOf(
                            CredentialRegistered(credentialId, accountId, FactorType.PASSWORD),
                            CredentialActivated(credentialId),
                        ),
                    )
                shouldThrow<InvalidStateTransition> {
                    credential.activate(ActivateCredential(command(), credentialId))
                }
            }
        }

        test("completa step-up em credencial ACTIVE") {
            runTest {
                val credential =
                    Credential.rehydrate(
                        listOf(
                            CredentialRegistered(credentialId, accountId, FactorType.PASSWORD),
                            CredentialActivated(credentialId),
                        ),
                    )
                credential
                    .completeStepUp(
                        CompleteStepUpChallenge(command(), credentialId, accountId, AssuranceLevel.SUBSTANTIAL, purpose),
                    ).single()
                    .shouldBeInstanceOf<StepUpChallengeCompleted>()
            }
        }

        test("rejeita step-up em credencial PENDING") {
            runTest {
                val credential =
                    Credential.rehydrate(
                        listOf(CredentialRegistered(credentialId, accountId, FactorType.PASSWORD)),
                    )
                shouldThrow<InvalidStateTransition> {
                    credential.completeStepUp(
                        CompleteStepUpChallenge(command(), credentialId, accountId, AssuranceLevel.SUBSTANTIAL, purpose),
                    )
                }
            }
        }

        test("revoga credencial ACTIVE para REVOKED") {
            runTest {
                val credential =
                    Credential.rehydrate(
                        listOf(
                            CredentialRegistered(credentialId, accountId, FactorType.PASSWORD),
                            CredentialActivated(credentialId),
                        ),
                    )
                credential
                    .revoke(RevokeCredential(command(), credentialId, "comprometida"))
                    .single()
                    .shouldBeInstanceOf<CredentialRevoked>()
            }
        }

        test("rejeita step-up em credencial REVOKED") {
            runTest {
                val credential =
                    Credential.rehydrate(
                        listOf(
                            CredentialRegistered(credentialId, accountId, FactorType.PASSWORD),
                            CredentialActivated(credentialId),
                            CredentialRevoked(credentialId, "x"),
                        ),
                    )
                shouldThrow<InvalidStateTransition> {
                    credential.completeStepUp(
                        CompleteStepUpChallenge(command(), credentialId, accountId, AssuranceLevel.SUBSTANTIAL, purpose),
                    )
                }
            }
        }
    })
