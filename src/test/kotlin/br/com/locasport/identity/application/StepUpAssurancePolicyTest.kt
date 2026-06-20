package br.com.locasport.identity.application

import br.com.locasport.identity.domain.AccountId
import br.com.locasport.identity.domain.AssuranceLevel
import br.com.locasport.identity.domain.CredentialId
import br.com.locasport.identity.domain.EventId
import br.com.locasport.identity.domain.PurposeReference
import br.com.locasport.identity.domain.RaiseAssuranceLevel
import br.com.locasport.identity.domain.StepUpChallengeCompleted
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import java.util.UUID

class StepUpAssurancePolicyTest :
    FunSpec({
        val credentialId = CredentialId.random()
        val accountId = AccountId.random()
        val purpose = PurposeReference("p")

        test("deriva RaiseAssuranceLevel a partir do step-up concluido") {
            runTest {
                val handler = mockk<RaiseAssuranceLevelHandler>(relaxed = true)
                val captured = slot<RaiseAssuranceLevel>()
                coEvery { handler.handle(capture(captured)) } returns CommandOutcome.Applied(1)
                val policy = StepUpAssurancePolicy(handler)

                val event =
                    StepUpChallengeCompleted(
                        credentialId,
                        accountId,
                        AssuranceLevel.HIGH,
                        purpose,
                        eventId = EventId(UUID.randomUUID()),
                    )
                policy.on(event)

                captured.captured.commandId.value shouldBe
                    UUID.nameUUIDFromBytes("step-up:${event.eventId.value}".toByteArray())
                captured.captured.accountId shouldBe event.accountId
                captured.captured.target shouldBe AssuranceLevel.HIGH
            }
        }

        test("eventos com mesmo eventId derivam o mesmo commandId") {
            runTest {
                val handler = mockk<RaiseAssuranceLevelHandler>(relaxed = true)
                val captured = mutableListOf<RaiseAssuranceLevel>()
                coEvery { handler.handle(capture(captured)) } returns CommandOutcome.Applied(1)
                val policy = StepUpAssurancePolicy(handler)

                val eventId = EventId(UUID.randomUUID())
                val first = StepUpChallengeCompleted(credentialId, accountId, AssuranceLevel.HIGH, purpose, eventId = eventId)
                val second = StepUpChallengeCompleted(credentialId, accountId, AssuranceLevel.HIGH, purpose, eventId = eventId)
                policy.on(first)
                policy.on(second)

                captured.size shouldBe 2
                captured[0].commandId shouldBe captured[1].commandId
            }
        }
    })
