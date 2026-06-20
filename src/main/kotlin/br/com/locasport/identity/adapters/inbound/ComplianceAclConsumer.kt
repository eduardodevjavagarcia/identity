package br.com.locasport.identity.adapters.inbound

import br.com.locasport.identity.adapters.config.LocalstackProperties
import br.com.locasport.identity.adapters.outbound.ComplianceReference
import br.com.locasport.identity.adapters.outbound.ComplianceReferenceProjection
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.util.UUID

class ComplianceAclConsumer(
    private val sqs: SqsAsyncClient,
    private val properties: LocalstackProperties,
    private val projection: ComplianceReferenceProjection,
) {
    private val mapper = ObjectMapper()

    suspend fun pollOnce(): Int {
        val response =
            sqs
                .receiveMessage { request ->
                    request.queueUrl(properties.aclQueueUrl)
                    request.maxNumberOfMessages(10)
                    request.waitTimeSeconds(1)
                }.await()
        var processed = 0
        for (message in response.messages()) {
            translate(message.body())?.let { projection.upsert(it) }
            sqs
                .deleteMessage { request ->
                    request.queueUrl(properties.aclQueueUrl)
                    request.receiptHandle(message.receiptHandle())
                }.await()
            processed += 1
        }
        return processed
    }

    suspend fun run() {
        while (true) {
            try {
                pollOnce()
            } catch (ex: Exception) {
            }
            delay(POLL_DELAY_MILLIS)
        }
    }

    private fun translate(body: String): ComplianceReference? {
        val node = mapper.readTree(body)
        return when (node.get("type")?.asText()) {
            "ConsentGranted" -> {
                val consentId = node.get("consentId").asText()
                ComplianceReference(UUID.fromString(consentId), "CONSENT", "consent:$consentId", "consent:$consentId")
            }

            "ProcessingRoleDefined" -> {
                val agreementId = node.get("agreementId").asText()
                ComplianceReference(
                    UUID.fromString(agreementId),
                    "PROCESSING_ROLE",
                    "agreement:$agreementId",
                    "agreement:$agreementId",
                )
            }

            else -> null
        }
    }

    private companion object {
        const val POLL_DELAY_MILLIS = 1000L
    }
}
