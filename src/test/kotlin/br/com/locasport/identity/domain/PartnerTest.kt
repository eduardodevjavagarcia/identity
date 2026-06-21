package br.com.locasport.identity.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import java.time.Instant
import java.util.UUID

class PartnerTest :
    FunSpec({
        val partnerId = PartnerId.random()
        val legalName = "Quadra SP Ltda"
        val taxId = "12345678000199"

        fun command() = CommandId(UUID.randomUUID())

        test("registra parceiro emite PartnerRegistered") {
            runTest {
                val partner = Partner.rehydrate(emptyList())
                val events =
                    partner.register(
                        RegisterPartner(
                            command(),
                            partnerId,
                            PartnerType.ARENA,
                            legalName,
                            taxId,
                            LegalBasis.CONTRACT_EXECUTION,
                        ),
                    )
                val registered = events.single().shouldBeInstanceOf<PartnerRegistered>()
                registered.partnerType shouldBe PartnerType.ARENA
                registered.legalName shouldBe legalName
            }
        }

        test("disclosePurpose em parceiro registrado emite PurposeDisclosed") {
            runTest {
                val partner =
                    Partner.rehydrate(
                        listOf(
                            PartnerRegistered(
                                partnerId,
                                PartnerType.ARENA,
                                legalName,
                                taxId,
                                LegalBasis.CONTRACT_EXECUTION,
                            ),
                        ),
                    )
                val events =
                    partner.disclosePurpose(DisclosePartnerPurpose(command(), partnerId, "arena"))
                events.single().shouldBeInstanceOf<PurposeDisclosed>()
            }
        }

        test("submete para revisao emite PartnerReviewSubmitted") {
            runTest {
                val partner =
                    Partner.rehydrate(
                        listOf(
                            PartnerRegistered(
                                partnerId,
                                PartnerType.ARENA,
                                legalName,
                                taxId,
                                LegalBasis.CONTRACT_EXECUTION,
                            ),
                        ),
                    )
                val events = partner.submitForReview(SubmitPartnerForReview(command(), partnerId))
                events.single().shouldBeInstanceOf<PartnerReviewSubmitted>()
            }
        }

        test("verifica identidade com VERIFIED emite PartnerIdentityVerified") {
            runTest {
                val verifiedAt = Instant.now()
                val partner =
                    Partner.rehydrate(
                        listOf(
                            PartnerRegistered(
                                partnerId,
                                PartnerType.ARENA,
                                legalName,
                                taxId,
                                LegalBasis.CONTRACT_EXECUTION,
                            ),
                            PurposeDisclosed(partnerId.value, "arena"),
                            PartnerReviewSubmitted(partnerId),
                            AssuranceLevelRaised(partnerId.value, AssuranceLevel.NONE, AssuranceLevel.VERIFIED),
                        ),
                    )
                val events = partner.verifyIdentity(VerifyPartnerIdentity(command(), partnerId, verifiedAt))
                val verified = events.single().shouldBeInstanceOf<PartnerIdentityVerified>()
                verified.assuranceLevel shouldBe AssuranceLevel.VERIFIED
                verified.legalName shouldBe legalName
                verified.taxId shouldBe taxId
            }
        }

        test("verifica identidade sem purpose dispara PurposeNotDisclosed") {
            runTest {
                val partner =
                    Partner.rehydrate(
                        listOf(
                            PartnerRegistered(
                                partnerId,
                                PartnerType.ARENA,
                                legalName,
                                taxId,
                                LegalBasis.CONTRACT_EXECUTION,
                            ),
                            PartnerReviewSubmitted(partnerId),
                            AssuranceLevelRaised(partnerId.value, AssuranceLevel.NONE, AssuranceLevel.VERIFIED),
                        ),
                    )
                shouldThrow<PurposeNotDisclosed> {
                    partner.verifyIdentity(VerifyPartnerIdentity(command(), partnerId, Instant.now()))
                }
            }
        }

        test("verifica identidade sem VERIFIED dispara PartnerApprovalDenied") {
            runTest {
                val partner =
                    Partner.rehydrate(
                        listOf(
                            PartnerRegistered(
                                partnerId,
                                PartnerType.ARENA,
                                legalName,
                                taxId,
                                LegalBasis.CONTRACT_EXECUTION,
                            ),
                            PurposeDisclosed(partnerId.value, "arena"),
                            PartnerReviewSubmitted(partnerId),
                        ),
                    )
                shouldThrow<PartnerApprovalDenied> {
                    partner.verifyIdentity(VerifyPartnerIdentity(command(), partnerId, Instant.now()))
                }
            }
        }

        test("rejeita parceiro em revisao emite PartnerRejected") {
            runTest {
                val partner =
                    Partner.rehydrate(
                        listOf(
                            PartnerRegistered(
                                partnerId,
                                PartnerType.ARENA,
                                legalName,
                                taxId,
                                LegalBasis.CONTRACT_EXECUTION,
                            ),
                            PartnerReviewSubmitted(partnerId),
                        ),
                    )
                val events = partner.reject(RejectPartner(command(), partnerId, "documentos invalidos"))
                events.single().shouldBeInstanceOf<PartnerRejected>()
            }
        }

        test("suspende parceiro ativo emite PartnerSuspended") {
            runTest {
                val partner =
                    Partner.rehydrate(
                        listOf(
                            PartnerRegistered(
                                partnerId,
                                PartnerType.ARENA,
                                legalName,
                                taxId,
                                LegalBasis.CONTRACT_EXECUTION,
                            ),
                            PurposeDisclosed(partnerId.value, "arena"),
                            PartnerReviewSubmitted(partnerId),
                            AssuranceLevelRaised(partnerId.value, AssuranceLevel.NONE, AssuranceLevel.VERIFIED),
                            PartnerIdentityVerified(
                                partnerId,
                                legalName,
                                taxId,
                                PartnerType.ARENA,
                                AssuranceLevel.VERIFIED,
                                Instant.now(),
                            ),
                        ),
                    )
                val events = partner.suspend(SuspendPartner(command(), partnerId, "fraude"))
                events.single().shouldBeInstanceOf<PartnerSuspended>()
            }
        }

        test("reativa parceiro suspenso emite PartnerReactivated") {
            runTest {
                val partner =
                    Partner.rehydrate(
                        listOf(
                            PartnerRegistered(
                                partnerId,
                                PartnerType.ARENA,
                                legalName,
                                taxId,
                                LegalBasis.CONTRACT_EXECUTION,
                            ),
                            PurposeDisclosed(partnerId.value, "arena"),
                            PartnerReviewSubmitted(partnerId),
                            AssuranceLevelRaised(partnerId.value, AssuranceLevel.NONE, AssuranceLevel.VERIFIED),
                            PartnerIdentityVerified(
                                partnerId,
                                legalName,
                                taxId,
                                PartnerType.ARENA,
                                AssuranceLevel.VERIFIED,
                                Instant.now(),
                            ),
                            PartnerSuspended(partnerId, "fraude"),
                        ),
                    )
                val events = partner.reactivate(ReactivatePartner(command(), partnerId))
                events.single().shouldBeInstanceOf<PartnerReactivated>()
            }
        }

        test("desativa parceiro ativo emite PartnerDeactivated") {
            runTest {
                val partner =
                    Partner.rehydrate(
                        listOf(
                            PartnerRegistered(
                                partnerId,
                                PartnerType.ARENA,
                                legalName,
                                taxId,
                                LegalBasis.CONTRACT_EXECUTION,
                            ),
                            PurposeDisclosed(partnerId.value, "arena"),
                            PartnerReviewSubmitted(partnerId),
                            AssuranceLevelRaised(partnerId.value, AssuranceLevel.NONE, AssuranceLevel.VERIFIED),
                            PartnerIdentityVerified(
                                partnerId,
                                legalName,
                                taxId,
                                PartnerType.ARENA,
                                AssuranceLevel.VERIFIED,
                                Instant.now(),
                            ),
                        ),
                    )
                val events = partner.deactivate(DeactivatePartner(command(), partnerId))
                events.single().shouldBeInstanceOf<PartnerDeactivated>()
            }
        }

        test("transicao invalida REGISTERED para ACTIVE salta UNDER_REVIEW") {
            runTest {
                val partner =
                    Partner.rehydrate(
                        listOf(
                            PartnerRegistered(
                                partnerId,
                                PartnerType.ARENA,
                                legalName,
                                taxId,
                                LegalBasis.CONTRACT_EXECUTION,
                            ),
                            PurposeDisclosed(partnerId.value, "arena"),
                        ),
                    )
                shouldThrow<InvalidStateTransition> {
                    partner.verifyIdentity(VerifyPartnerIdentity(command(), partnerId, Instant.now()))
                }
            }
        }

        test("grantRole idempotente em parceiro ativo") {
            runTest {
                val partner =
                    Partner.rehydrate(
                        listOf(
                            PartnerRegistered(
                                partnerId,
                                PartnerType.ARENA,
                                legalName,
                                taxId,
                                LegalBasis.CONTRACT_EXECUTION,
                            ),
                            PurposeDisclosed(partnerId.value, "arena"),
                            PartnerReviewSubmitted(partnerId),
                            AssuranceLevelRaised(partnerId.value, AssuranceLevel.NONE, AssuranceLevel.VERIFIED),
                            PartnerIdentityVerified(
                                partnerId,
                                legalName,
                                taxId,
                                PartnerType.ARENA,
                                AssuranceLevel.VERIFIED,
                                Instant.now(),
                            ),
                        ),
                    )
                partner
                    .grantRole(GrantPartnerRole(command(), partnerId, Role.ARENA_OPERATOR))
                    .single()
                    .shouldBeInstanceOf<RoleGranted>()

                val withRole =
                    Partner.rehydrate(
                        listOf(
                            PartnerRegistered(
                                partnerId,
                                PartnerType.ARENA,
                                legalName,
                                taxId,
                                LegalBasis.CONTRACT_EXECUTION,
                            ),
                            PurposeDisclosed(partnerId.value, "arena"),
                            PartnerReviewSubmitted(partnerId),
                            AssuranceLevelRaised(partnerId.value, AssuranceLevel.NONE, AssuranceLevel.VERIFIED),
                            PartnerIdentityVerified(
                                partnerId,
                                legalName,
                                taxId,
                                PartnerType.ARENA,
                                AssuranceLevel.VERIFIED,
                                Instant.now(),
                            ),
                            RoleGranted(partnerId.value, Role.ARENA_OPERATOR),
                        ),
                    )
                withRole.grantRole(
                    GrantPartnerRole(command(), partnerId, Role.ARENA_OPERATOR),
                ) shouldBe emptyList()
            }
        }
    })
