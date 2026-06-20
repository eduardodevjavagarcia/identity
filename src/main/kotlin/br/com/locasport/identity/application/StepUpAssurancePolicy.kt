package br.com.locasport.identity.application

import br.com.locasport.identity.domain.CommandId
import br.com.locasport.identity.domain.RaiseAssuranceLevel
import br.com.locasport.identity.domain.StepUpChallengeCompleted
import java.util.UUID

class StepUpAssurancePolicy(
    private val raiseAssurance: RaiseAssuranceLevelHandler,
) {
    suspend fun on(event: StepUpChallengeCompleted): CommandOutcome =
        raiseAssurance.handle(
            RaiseAssuranceLevel(
                CommandId(UUID.nameUUIDFromBytes("step-up:${event.eventId.value}".toByteArray())),
                event.accountId,
                event.achievedAssurance,
                event.purposeReference,
            ),
        )
}
