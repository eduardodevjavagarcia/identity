package br.com.locasport.identity.integration

import br.com.locasport.identity.adapters.config.EventStoreProperties
import br.com.locasport.identity.adapters.config.LocalstackProperties
import br.com.locasport.identity.adapters.inbound.ComplianceAclConsumer
import br.com.locasport.identity.adapters.outbound.AccountProjection
import br.com.locasport.identity.adapters.outbound.AvroEventCodec
import br.com.locasport.identity.adapters.outbound.ComplianceReferenceProjection
import br.com.locasport.identity.adapters.outbound.CredentialProjection
import br.com.locasport.identity.adapters.outbound.PostgresCommandDeduplication
import br.com.locasport.identity.adapters.outbound.R2dbcEventStore
import br.com.locasport.identity.adapters.outbound.SqsFifoEventPublisher
import br.com.locasport.identity.application.ActivateCredentialHandler
import br.com.locasport.identity.application.AssignRoleHandler
import br.com.locasport.identity.application.CommandOutcome
import br.com.locasport.identity.application.RaiseAssuranceLevelHandler
import br.com.locasport.identity.application.RegisterAccountHandler
import br.com.locasport.identity.application.RegisterCredentialHandler
import br.com.locasport.identity.application.ReinstateAccountHandler
import br.com.locasport.identity.application.RevokeRoleHandler
import br.com.locasport.identity.application.StepUpAssurancePolicy
import br.com.locasport.identity.application.SuspendAccountHandler
import br.com.locasport.identity.domain.AccountId
import br.com.locasport.identity.domain.ActivateCredential
import br.com.locasport.identity.domain.AssignRole
import br.com.locasport.identity.domain.AssuranceLevel
import br.com.locasport.identity.domain.AssuranceLevelRaised
import br.com.locasport.identity.domain.CommandId
import br.com.locasport.identity.domain.CredentialId
import br.com.locasport.identity.domain.EventId
import br.com.locasport.identity.domain.FactorType
import br.com.locasport.identity.domain.InvalidStateTransition
import br.com.locasport.identity.domain.PurposeReference
import br.com.locasport.identity.domain.RaiseAssuranceLevel
import br.com.locasport.identity.domain.RegisterAccount
import br.com.locasport.identity.domain.RegisterCredential
import br.com.locasport.identity.domain.ReinstateAccount
import br.com.locasport.identity.domain.RevokeRole
import br.com.locasport.identity.domain.Role
import br.com.locasport.identity.domain.StepUpChallengeCompleted
import br.com.locasport.identity.domain.StreamId
import br.com.locasport.identity.domain.SubjectType
import br.com.locasport.identity.domain.SuspendAccount
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.r2dbc.spi.ConnectionFactories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitOneOrNull
import org.springframework.r2dbc.core.flow
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.net.URI
import java.time.Instant
import java.util.UUID

class IdentityIntegrationTest :
    FunSpec({
        val connectionFactory =
            ConnectionFactories.get("r2dbc:postgresql://identity:identity@localhost:5432/identity")
        val client = DatabaseClient.create(connectionFactory)
        val sqs =
            SqsAsyncClient
                .builder()
                .endpointOverride(URI.create("http://localhost:4566"))
                .region(Region.of("us-east-1"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build()
        val sns =
            SnsAsyncClient
                .builder()
                .endpointOverride(URI.create("http://localhost:4566"))
                .region(Region.of("us-east-1"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build()
        val eventsQueueUrl = sqs.getQueueUrl { it.queueName("identity-events.fifo") }.get().queueUrl()
        val aclQueueUrl = sqs.getQueueUrl { it.queueName("identity-compliance-acl.fifo") }.get().queueUrl()
        val topicArn = "arn:aws:sns:us-east-1:000000000000:compliance-events.fifo"
        val properties =
            LocalstackProperties("http://localhost:4566", "us-east-1", "test", "test", eventsQueueUrl, aclQueueUrl)
        val mapper = ObjectMapper()

        val codec = AvroEventCodec()
        val eventStore = R2dbcEventStore(client, codec, EventStoreProperties())
        val deduplication = PostgresCommandDeduplication(client)
        val publisher = SqsFifoEventPublisher(sqs, properties)
        val accountProjection = AccountProjection(client)
        val credentialProjection = CredentialProjection(client)
        val referenceProjection = ComplianceReferenceProjection(client)

        val register = RegisterAccountHandler(deduplication, eventStore, accountProjection, publisher)
        val raiseAssurance = RaiseAssuranceLevelHandler(deduplication, eventStore, accountProjection, publisher)
        val assignRole = AssignRoleHandler(deduplication, eventStore, accountProjection, publisher)
        val revokeRole = RevokeRoleHandler(deduplication, eventStore, accountProjection, publisher)
        val suspendAccount = SuspendAccountHandler(deduplication, eventStore, accountProjection, publisher)
        val reinstate = ReinstateAccountHandler(deduplication, eventStore, accountProjection, publisher)
        val registerCredential = RegisterCredentialHandler(deduplication, eventStore, credentialProjection, publisher)
        val activateCredential = ActivateCredentialHandler(deduplication, eventStore, credentialProjection, publisher)
        val policy = StepUpAssurancePolicy(raiseAssurance)
        val aclConsumer = ComplianceAclConsumer(sqs, properties, referenceProjection)

        afterSpec {
            sqs.close()
            sns.close()
        }

        test("descarta comando duplicado e persiste apenas um evento sob carga concorrente") {
            val accountId = AccountId.random()
            val command =
                RegisterAccount(CommandId(UUID.randomUUID()), accountId, SubjectType.PERSON, PurposeReference("p"))

            val outcomes =
                coroutineScope {
                    (1..50).map { async(Dispatchers.IO) { register.handle(command) } }.awaitAll()
                }

            outcomes.count { it is CommandOutcome.Applied } shouldBe 1
            countEvents(client, accountId.value) shouldBe 1L
        }

        test("preserva ordem FIFO dos eventos do stream na SQS sob carga") {
            val accountId = AccountId.random()
            val purpose = PurposeReference("p")
            register.handle(RegisterAccount(CommandId(UUID.randomUUID()), accountId, SubjectType.PERSON, purpose))
            raiseAssurance.handle(
                RaiseAssuranceLevel(CommandId(UUID.randomUUID()), accountId, AssuranceLevel.SUBSTANTIAL, purpose),
            )
            assignRole.handle(AssignRole(CommandId(UUID.randomUUID()), accountId, Role.PRACTITIONER))

            eventStore.load(StreamId.of(accountId)).map { it.event.type } shouldBe
                listOf("AccountRegistered", "AssuranceLevelRaised", "RoleAssigned")

            drainTypesForStream(sqs, eventsQueueUrl, accountId.value.toString(), mapper, expected = 3) shouldBe
                listOf("AccountRegistered", "AssuranceLevelRaised", "RoleAssigned")
        }

        test("rejeita transicao invalida end-to-end (REGISTERED->VERIFIED)") {
            val accountId = AccountId.random()
            val purpose = PurposeReference("p")
            register.handle(RegisterAccount(CommandId(UUID.randomUUID()), accountId, SubjectType.PERSON, purpose))

            shouldThrow<InvalidStateTransition> {
                raiseAssurance.handle(RaiseAssuranceLevel(CommandId(UUID.randomUUID()), accountId, AssuranceLevel.HIGH, purpose))
            }
        }

        test("idempotencia cross-stream do StepUpAssurancePolicy") {
            val accountId = AccountId.random()
            val credentialId = CredentialId.random()
            val purpose = PurposeReference("p")
            register.handle(RegisterAccount(CommandId(UUID.randomUUID()), accountId, SubjectType.PERSON, purpose))
            raiseAssurance.handle(
                RaiseAssuranceLevel(CommandId(UUID.randomUUID()), accountId, AssuranceLevel.SUBSTANTIAL, purpose),
            )
            registerCredential.handle(
                RegisterCredential(CommandId(UUID.randomUUID()), credentialId, accountId, FactorType.PASSWORD),
            )
            activateCredential.handle(ActivateCredential(CommandId(UUID.randomUUID()), credentialId))

            val stepUp =
                StepUpChallengeCompleted(
                    credentialId,
                    accountId,
                    AssuranceLevel.HIGH,
                    purpose,
                    eventId = EventId(UUID.randomUUID()),
                )
            val first = policy.on(stepUp)
            val second = policy.on(stepUp)

            first.shouldBeInstanceOf<CommandOutcome.Applied>()
            second shouldBe CommandOutcome.Discarded

            eventStore.load(StreamId.of(accountId)).count {
                it.event is AssuranceLevelRaised && (it.event as AssuranceLevelRaised).to == AssuranceLevel.HIGH
            } shouldBe 1
        }

        test("consome evento do compliance via ACL e atualiza projecao de referencia") {
            val consentId = UUID.randomUUID()
            val body =
                mapper.writeValueAsString(
                    linkedMapOf(
                        "type" to "ConsentGranted",
                        "eventId" to UUID.randomUUID().toString(),
                        "stream" to consentId.toString(),
                        "occurredAt" to Instant.now().toString(),
                        "consentId" to consentId.toString(),
                        "purpose" to "marketing",
                        "legalBasis" to "CONSENT",
                        "category" to "COMMON",
                    ),
                )
            sns
                .publish { request ->
                    request.topicArn(topicArn)
                    request.message(body)
                    request.messageGroupId(consentId.toString())
                    request.messageDeduplicationId(UUID.randomUUID().toString())
                }.get()

            var processed = 0
            var attempts = 0
            while (processed == 0 && attempts < 20) {
                processed += aclConsumer.pollOnce()
                attempts += 1
            }

            val reference = loadReference(client, consentId).shouldNotBeNull()
            reference.sourceType shouldBe "CONSENT"
            reference.purposeReference shouldBe "consent:$consentId"
        }

        test("revoga papel atribuido e remove da projecao") {
            val accountId = AccountId.random()
            val purpose = PurposeReference("p")
            register.handle(RegisterAccount(CommandId(UUID.randomUUID()), accountId, SubjectType.PERSON, purpose))
            assignRole.handle(AssignRole(CommandId(UUID.randomUUID()), accountId, Role.PRACTITIONER))
            revokeRole.handle(RevokeRole(CommandId(UUID.randomUUID()), accountId, Role.PRACTITIONER))

            eventStore.load(StreamId.of(accountId)).map { it.event.type } shouldBe
                listOf("AccountRegistered", "RoleAssigned", "RoleRevoked")

            loadAccountRoles(client, accountId.value) shouldBe ""
        }

        test("reinstala conta suspensa end-to-end") {
            val accountId = AccountId.random()
            val purpose = PurposeReference("p")
            register.handle(RegisterAccount(CommandId(UUID.randomUUID()), accountId, SubjectType.PERSON, purpose))
            suspendAccount.handle(SuspendAccount(CommandId(UUID.randomUUID()), accountId, "motivo"))
            reinstate.handle(ReinstateAccount(CommandId(UUID.randomUUID()), accountId))

            eventStore.load(StreamId.of(accountId)).map { it.event.type } shouldBe
                listOf("AccountRegistered", "AccountSuspended", "AccountReinstated")

            loadAccountStatus(client, accountId.value) shouldBe "REGISTERED"
        }
    })

private data class ReferenceRow(
    val sourceType: String,
    val purposeReference: String,
)

private suspend fun countEvents(
    client: DatabaseClient,
    streamId: UUID,
): Long =
    client
        .sql("SELECT count(*) AS total FROM identity_event WHERE stream_id = :id")
        .bind("id", streamId)
        .map { row -> row.get("total", java.lang.Long::class.java)!!.toLong() }
        .flow()
        .toList()
        .first()

private suspend fun loadReference(
    client: DatabaseClient,
    referenceId: UUID,
): ReferenceRow? =
    client
        .sql("SELECT source_type, purpose_reference FROM compliance_reference_view WHERE reference_id = :id")
        .bind("id", referenceId)
        .map { row ->
            ReferenceRow(
                row.get("source_type", String::class.java)!!,
                row.get("purpose_reference", String::class.java)!!,
            )
        }.awaitOneOrNull()

private suspend fun loadAccountRoles(
    client: DatabaseClient,
    accountId: UUID,
): String? =
    client
        .sql("SELECT roles FROM account_view WHERE account_id = :id")
        .bind("id", accountId)
        .map { row -> row.get("roles", String::class.java) ?: "" }
        .awaitOneOrNull()

private suspend fun loadAccountStatus(
    client: DatabaseClient,
    accountId: UUID,
): String? =
    client
        .sql("SELECT status FROM account_view WHERE account_id = :id")
        .bind("id", accountId)
        .map { row -> row.get("status", String::class.java)!! }
        .awaitOneOrNull()

private fun drainTypesForStream(
    sqs: SqsAsyncClient,
    queueUrl: String,
    streamId: String,
    mapper: ObjectMapper,
    expected: Int,
): List<String> {
    val types = mutableListOf<String>()
    var attempts = 0
    while (types.size < expected && attempts < 20) {
        attempts += 1
        val response =
            sqs
                .receiveMessage { request ->
                    request.queueUrl(queueUrl)
                    request.maxNumberOfMessages(10)
                    request.waitTimeSeconds(1)
                }.get()
        for (message in response.messages()) {
            val node = mapper.readTree(message.body())
            if (node.get("stream").asText() == streamId) {
                types.add(node.get("type").asText())
            }
            sqs.deleteMessage { it.queueUrl(queueUrl).receiptHandle(message.receiptHandle()) }.get()
        }
    }
    return types
}
