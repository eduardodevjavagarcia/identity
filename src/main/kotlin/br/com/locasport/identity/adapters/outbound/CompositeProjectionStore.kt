package br.com.locasport.identity.adapters.outbound

import br.com.locasport.identity.domain.DomainEvent
import br.com.locasport.identity.domain.ProjectionStore

class CompositeProjectionStore(
    private val delegates: List<ProjectionStore>,
) : ProjectionStore {
    override suspend fun apply(events: List<DomainEvent>) {
        for (delegate in delegates) {
            delegate.apply(events)
        }
    }
}
