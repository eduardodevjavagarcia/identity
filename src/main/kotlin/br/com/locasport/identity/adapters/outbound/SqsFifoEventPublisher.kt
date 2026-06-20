package br.com.locasport.identity.adapters.outbound

import br.com.locasport.identity.adapters.config.LocalstackProperties
import br.com.locasport.identity.domain.DomainEvent
import br.com.locasport.identity.domain.EventPublisher
import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.sqs.SqsAsyncClient

class SqsFifoEventPublisher(
    private val sqs: SqsAsyncClient,
    private val properties: LocalstackProperties,
) : EventPublisher {
    override suspend fun publish(events: List<DomainEvent>) {
        for (event in events) {
            sqs
                .sendMessage { request ->
                    request.queueUrl(properties.eventsQueueUrl)
                    request.messageGroupId(event.stream.value.toString())
                    request.messageDeduplicationId(event.eventId.value.toString())
                    request.messageBody(JsonEventCodec.encode(event))
                }.await()
        }
    }
}
