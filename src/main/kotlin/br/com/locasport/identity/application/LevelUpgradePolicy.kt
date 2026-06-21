package br.com.locasport.identity.application

import br.com.locasport.identity.domain.AssuranceLevel
import java.util.UUID

interface LevelUpgradePolicy {
    suspend fun mayUpgradeTo(
        subjectId: UUID,
        target: AssuranceLevel,
    ): Boolean
}

class DefaultLevelUpgradePolicy : LevelUpgradePolicy {
    override suspend fun mayUpgradeTo(
        subjectId: UUID,
        target: AssuranceLevel,
    ): Boolean = target != AssuranceLevel.VERIFIED
}
