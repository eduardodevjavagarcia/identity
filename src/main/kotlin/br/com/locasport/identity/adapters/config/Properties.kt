package br.com.locasport.identity.adapters.config

import java.time.Duration

data class IdentityProperties(
    val eventStore: EventStoreProperties,
    val localstack: LocalstackProperties,
    val retention: RetentionProperties,
)

data class EventStoreProperties(
    val table: String = "identity_event",
)

data class LocalstackProperties(
    val endpoint: String,
    val region: String,
    val accessKey: String,
    val secretKey: String,
    val eventsQueueUrl: String,
    val aclQueueUrl: String,
)

data class RetentionProperties(
    val hotWindow: Duration,
    val archiveBucket: String,
)
