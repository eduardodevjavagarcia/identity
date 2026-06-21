package br.com.locasport.identity.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import java.util.UUID

class PersonTest :
    FunSpec({
        val personId = PersonId.random()

        fun command() = CommandId(UUID.randomUUID())

        test("registra pessoa emite PersonRegistered") {
            runTest {
                val person = Person.rehydrate(emptyList())
                val events = person.register(RegisterPerson(command(), personId, LegalBasis.CONTRACT_EXECUTION))
                events.single().shouldBeInstanceOf<PersonRegistered>()
            }
        }

        test("disclosePurpose em pessoa registrada emite PurposeDisclosed") {
            runTest {
                val person =
                    Person.rehydrate(listOf(PersonRegistered(personId, LegalBasis.CONTRACT_EXECUTION)))
                val events = person.disclosePurpose(DisclosePurpose(command(), personId, "treino"))
                events.single().shouldBeInstanceOf<PurposeDisclosed>()
            }
        }

        test("disclosePurpose idempotente retorna vazio") {
            runTest {
                val person =
                    Person.rehydrate(
                        listOf(
                            PersonRegistered(personId, LegalBasis.CONTRACT_EXECUTION),
                            PurposeDisclosed(personId.value, "treino"),
                        ),
                    )
                person.disclosePurpose(DisclosePurpose(command(), personId, "treino")) shouldBe emptyList()
            }
        }

        test("raiseAssurance sem purpose dispara PurposeNotDisclosed") {
            runTest {
                val person =
                    Person.rehydrate(listOf(PersonRegistered(personId, LegalBasis.CONTRACT_EXECUTION)))
                shouldThrow<PurposeNotDisclosed> {
                    person.raiseAssurance(RaiseAssuranceLevel(command(), personId.value, AssuranceLevel.BASIC))
                }
            }
        }

        test("raiseAssurance NONE para BASIC sucesso") {
            runTest {
                val person =
                    Person.rehydrate(
                        listOf(
                            PersonRegistered(personId, LegalBasis.CONTRACT_EXECUTION),
                            PurposeDisclosed(personId.value, "treino"),
                        ),
                    )
                val raised =
                    person
                        .raiseAssurance(RaiseAssuranceLevel(command(), personId.value, AssuranceLevel.BASIC))
                        .single()
                        .shouldBeInstanceOf<AssuranceLevelRaised>()
                raised.to shouldBe AssuranceLevel.BASIC
            }
        }

        test("raiseAssurance para VERIFIED sucesso (politica delegada ao handler)") {
            runTest {
                val person =
                    Person.rehydrate(
                        listOf(
                            PersonRegistered(personId, LegalBasis.CONTRACT_EXECUTION),
                            PurposeDisclosed(personId.value, "treino"),
                            AssuranceLevelRaised(personId.value, AssuranceLevel.NONE, AssuranceLevel.BASIC),
                        ),
                    )
                val raised =
                    person
                        .raiseAssurance(RaiseAssuranceLevel(command(), personId.value, AssuranceLevel.VERIFIED))
                        .single()
                        .shouldBeInstanceOf<AssuranceLevelRaised>()
                raised.to shouldBe AssuranceLevel.VERIFIED
            }
        }

        test("rejeita downgrade de assurance") {
            runTest {
                val person =
                    Person.rehydrate(
                        listOf(
                            PersonRegistered(personId, LegalBasis.CONTRACT_EXECUTION),
                            PurposeDisclosed(personId.value, "treino"),
                            AssuranceLevelRaised(personId.value, AssuranceLevel.NONE, AssuranceLevel.BASIC),
                        ),
                    )
                shouldThrow<InvalidAssuranceDowngrade> {
                    person.raiseAssurance(RaiseAssuranceLevel(command(), personId.value, AssuranceLevel.NONE))
                }
            }
        }

        test("ativa conta com BASIC sucesso") {
            runTest {
                val person =
                    Person.rehydrate(
                        listOf(
                            PersonRegistered(personId, LegalBasis.CONTRACT_EXECUTION),
                            PurposeDisclosed(personId.value, "treino"),
                            AssuranceLevelRaised(personId.value, AssuranceLevel.NONE, AssuranceLevel.BASIC),
                        ),
                    )
                val activated =
                    person.activate(ActivateAccount(command(), personId)).single().shouldBeInstanceOf<PersonActivated>()
                activated.personId shouldBe personId
            }
        }

        test("ativa conta sem BASIC dispara AccountActivationDenied") {
            runTest {
                val person =
                    Person.rehydrate(listOf(PersonRegistered(personId, LegalBasis.CONTRACT_EXECUTION)))
                shouldThrow<AccountActivationDenied> {
                    person.activate(ActivateAccount(command(), personId))
                }
            }
        }

        test("suspende conta ativa emite PersonSuspended") {
            runTest {
                val person =
                    Person.rehydrate(
                        listOf(
                            PersonRegistered(personId, LegalBasis.CONTRACT_EXECUTION),
                            PurposeDisclosed(personId.value, "treino"),
                            AssuranceLevelRaised(personId.value, AssuranceLevel.NONE, AssuranceLevel.BASIC),
                            PersonActivated(personId),
                        ),
                    )
                person
                    .suspend(SuspendPerson(command(), personId, "abuso"))
                    .single()
                    .shouldBeInstanceOf<PersonSuspended>()
            }
        }

        test("reativa conta suspensa emite PersonReactivated") {
            runTest {
                val person =
                    Person.rehydrate(
                        listOf(
                            PersonRegistered(personId, LegalBasis.CONTRACT_EXECUTION),
                            PurposeDisclosed(personId.value, "treino"),
                            AssuranceLevelRaised(personId.value, AssuranceLevel.NONE, AssuranceLevel.BASIC),
                            PersonActivated(personId),
                            PersonSuspended(personId, "abuso"),
                        ),
                    )
                person
                    .reactivate(ReactivatePerson(command(), personId))
                    .single()
                    .shouldBeInstanceOf<PersonReactivated>()
            }
        }

        test("desativa conta ativa emite PersonDeactivated") {
            runTest {
                val person =
                    Person.rehydrate(
                        listOf(
                            PersonRegistered(personId, LegalBasis.CONTRACT_EXECUTION),
                            PurposeDisclosed(personId.value, "treino"),
                            AssuranceLevelRaised(personId.value, AssuranceLevel.NONE, AssuranceLevel.BASIC),
                            PersonActivated(personId),
                        ),
                    )
                person
                    .deactivate(DeactivatePerson(command(), personId))
                    .single()
                    .shouldBeInstanceOf<PersonDeactivated>()
            }
        }

        test("grantRole idempotente") {
            runTest {
                val person =
                    Person.rehydrate(
                        listOf(
                            PersonRegistered(personId, LegalBasis.CONTRACT_EXECUTION),
                            PurposeDisclosed(personId.value, "treino"),
                            AssuranceLevelRaised(personId.value, AssuranceLevel.NONE, AssuranceLevel.BASIC),
                            PersonActivated(personId),
                        ),
                    )
                person
                    .grantRole(GrantRole(command(), personId, Role.PRACTITIONER))
                    .single()
                    .shouldBeInstanceOf<RoleGranted>()

                val withRole =
                    Person.rehydrate(
                        listOf(
                            PersonRegistered(personId, LegalBasis.CONTRACT_EXECUTION),
                            PurposeDisclosed(personId.value, "treino"),
                            AssuranceLevelRaised(personId.value, AssuranceLevel.NONE, AssuranceLevel.BASIC),
                            PersonActivated(personId),
                            RoleGranted(personId.value, Role.PRACTITIONER),
                        ),
                    )
                withRole.grantRole(GrantRole(command(), personId, Role.PRACTITIONER)) shouldBe emptyList()
            }
        }

        test("revokeRole emite RoleRevoked") {
            runTest {
                val person =
                    Person.rehydrate(
                        listOf(
                            PersonRegistered(personId, LegalBasis.CONTRACT_EXECUTION),
                            PurposeDisclosed(personId.value, "treino"),
                            AssuranceLevelRaised(personId.value, AssuranceLevel.NONE, AssuranceLevel.BASIC),
                            PersonActivated(personId),
                            RoleGranted(personId.value, Role.PRACTITIONER),
                        ),
                    )
                person
                    .revokeRole(RevokeRole(command(), personId, Role.PRACTITIONER))
                    .single()
                    .shouldBeInstanceOf<RoleRevoked>()
            }
        }

        test("revogar papel inexistente retorna vazio") {
            runTest {
                val person =
                    Person.rehydrate(
                        listOf(
                            PersonRegistered(personId, LegalBasis.CONTRACT_EXECUTION),
                            PurposeDisclosed(personId.value, "treino"),
                            AssuranceLevelRaised(personId.value, AssuranceLevel.NONE, AssuranceLevel.BASIC),
                            PersonActivated(personId),
                        ),
                    )
                person.revokeRole(RevokeRole(command(), personId, Role.PRACTITIONER)) shouldBe emptyList()
            }
        }
    })
