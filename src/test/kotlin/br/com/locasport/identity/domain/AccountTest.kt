package br.com.locasport.identity.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import java.util.UUID

class AccountTest :
    FunSpec({
        val accountId = AccountId.random()
        val purpose = PurposeReference("p")

        fun command() = CommandId(UUID.randomUUID())

        test("eleva assurance para SUBSTANTIAL a partir de conta registrada") {
            runTest {
                val account = Account.rehydrate(listOf(AccountRegistered(accountId, SubjectType.PERSON, purpose)))
                val raised =
                    account
                        .raiseAssurance(RaiseAssuranceLevel(command(), accountId, AssuranceLevel.SUBSTANTIAL, purpose))
                        .single()
                        .shouldBeInstanceOf<AssuranceLevelRaised>()
                raised.to shouldBe AssuranceLevel.SUBSTANTIAL
            }
        }

        test("rejeita salto REGISTERED para VERIFIED") {
            runTest {
                val account = Account.rehydrate(listOf(AccountRegistered(accountId, SubjectType.PERSON, purpose)))
                shouldThrow<InvalidStateTransition> {
                    account.raiseAssurance(RaiseAssuranceLevel(command(), accountId, AssuranceLevel.HIGH, purpose))
                }
            }
        }

        test("rejeita downgrade de assurance") {
            runTest {
                val account =
                    Account.rehydrate(
                        listOf(
                            AccountRegistered(accountId, SubjectType.PERSON, purpose),
                            AssuranceLevelRaised(accountId, AssuranceLevel.NONE, AssuranceLevel.SUBSTANTIAL, purpose),
                        ),
                    )
                shouldThrow<InvalidAssuranceDowngrade> {
                    account.raiseAssurance(RaiseAssuranceLevel(command(), accountId, AssuranceLevel.LOW, purpose))
                }
            }
        }

        test("atinge VERIFIED via IDENTIFIED") {
            runTest {
                val account =
                    Account.rehydrate(
                        listOf(
                            AccountRegistered(accountId, SubjectType.PERSON, purpose),
                            AssuranceLevelRaised(accountId, AssuranceLevel.NONE, AssuranceLevel.SUBSTANTIAL, purpose),
                        ),
                    )
                val raised =
                    account
                        .raiseAssurance(RaiseAssuranceLevel(command(), accountId, AssuranceLevel.HIGH, purpose))
                        .single()
                        .shouldBeInstanceOf<AssuranceLevelRaised>()
                raised.to shouldBe AssuranceLevel.HIGH
            }
        }

        test("rejeita raiseAssurance em conta suspensa") {
            runTest {
                val account =
                    Account.rehydrate(
                        listOf(
                            AccountRegistered(accountId, SubjectType.PERSON, purpose),
                            AccountSuspended(accountId, "motivo"),
                        ),
                    )
                shouldThrow<InvalidStateTransition> {
                    account.raiseAssurance(RaiseAssuranceLevel(command(), accountId, AssuranceLevel.SUBSTANTIAL, purpose))
                }
            }
        }

        test("assignRole eh idempotente") {
            runTest {
                val registered = Account.rehydrate(listOf(AccountRegistered(accountId, SubjectType.PERSON, purpose)))
                registered
                    .assignRole(AssignRole(command(), accountId, Role.PRACTITIONER))
                    .single()
                    .shouldBeInstanceOf<RoleAssigned>()

                val withRole =
                    Account.rehydrate(
                        listOf(
                            AccountRegistered(accountId, SubjectType.PERSON, purpose),
                            RoleAssigned(accountId, Role.PRACTITIONER),
                        ),
                    )
                withRole.assignRole(AssignRole(command(), accountId, Role.PRACTITIONER)) shouldBe emptyList<DomainEvent>()
            }
        }

        test("reinstala conta suspensa") {
            runTest {
                val account =
                    Account.rehydrate(
                        listOf(
                            AccountRegistered(accountId, SubjectType.PERSON, purpose),
                            AccountSuspended(accountId, "motivo"),
                        ),
                    )
                account
                    .reinstate(ReinstateAccount(command(), accountId))
                    .single()
                    .shouldBeInstanceOf<AccountReinstated>()
            }
        }

        test("revoga papel atribuido emite RoleRevoked") {
            runTest {
                val account =
                    Account.rehydrate(
                        listOf(
                            AccountRegistered(accountId, SubjectType.PERSON, purpose),
                            RoleAssigned(accountId, Role.PRACTITIONER),
                        ),
                    )
                account
                    .revokeRole(RevokeRole(command(), accountId, Role.PRACTITIONER))
                    .single()
                    .shouldBeInstanceOf<RoleRevoked>()
            }
        }

        test("revogar papel inexistente retorna vazio") {
            runTest {
                val account = Account.rehydrate(listOf(AccountRegistered(accountId, SubjectType.PERSON, purpose)))
                account.revokeRole(RevokeRole(command(), accountId, Role.PRACTITIONER)) shouldBe emptyList<DomainEvent>()
            }
        }
    })
