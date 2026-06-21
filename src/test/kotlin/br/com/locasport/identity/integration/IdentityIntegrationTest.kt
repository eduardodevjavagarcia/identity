package br.com.locasport.identity.integration

import br.com.locasport.identity.adapters.config.EventStoreProperties
import br.com.locasport.identity.adapters.config.LocalstackProperties
import br.com.locasport.identity.adapters.inbound.ComplianceAclConsumer
import br.com.locasport.identity.adapters.outbound.AvroEventCodec
import br.com.locasport.identity.adapters.outbound.ComplianceReferenceProjection
import br.com.locasport.identity.adapters.outbound.CompositeProjectionStore
import br.com.locasport.identity.adapters.outbound.IdentityAssuranceProjection
import br.com.locasport.identity.adapters.outbound.PartnerProjection
import br.com.locasport.identity.adapters.outbound.PersonProjection
import br.com.locasport.identity.adapters.outbound.PostgresCommandDeduplication
import br.com.locasport.identity.adapters.outbound.R2dbcEventStore
import br.com.locasport.identity.adapters.outbound.SqsFifoEventPublisher
import br.com.locasport.identity.application.ActivatePersonAccountHandler
import br.com.locasport.identity.application.CommandOutcome
import br.com.locasport.identity.application.DefaultLevelUpgradePolicy
import br.com.locasport.identity.application.DisclosePartnerPurposeHandler
import br.com.locasport.identity.application.DisclosePersonPurposeHandler
import br.com.locasport.identity.application.GrantPersonRoleHandler
import br.com.locasport.identity.application.LevelUpgradePolicy
import br.com.locasport.identity.application.RaisePartnerAssuranceLevelHandler
import br.com.locasport.identity.application.RaisePersonAssuranceLevelHandler
import br.com.locasport.identity.application.ReactivatePersonHandler
import br.com.locasport.identity.application.RegisterPartnerHandler
import br.com.locasport.identity.application.RegisterPersonHandler
import br.com.locasport.identity.application.RevokePersonRoleHandler
import br.com.locasport.identity.application.SubmitPartnerForReviewHandler
import br.com.locasport.identity.application.SuspendPersonHandler
import br.com.locasport.identity.application.VerifyPartnerIdentityHandler
import br.com.locasport.identity.domain.AccountActivationDenied
import br.com.locasport.identity.domain.ActivateAccount
import br.com.locasport.identity.domain.AssuranceLevel
import br.com.locasport.identity.domain.CommandId
import br.com.locasport.identity.domain.DisclosePartnerPurpose
import br.com.locasport.identity.domain.DisclosePurpose
import br.com.locasport.identity.domain.ExcessiveAssuranceDenied
import br.com.locasport.identity.domain.GrantRole
import br.com.locasport.identity.domain.InvalidAssuranceDowngrade
import br.com.locasport.identity.domain.LegalBasis
import br.com.locasport.identity.domain.PartnerId
import br.com.locasport.identity.domain.PartnerType
import br.com.locasport.identity.domain.PersonId
import br.com.locasport.identity.domain.PurposeNotDisclosed
import br.com.locasport.identity.domain.RaiseAssuranceLevel
import br.com.locasport.identity.domain.ReactivatePerson
import br.com.locasport.identity.domain.RegisterPartner
import br.com.locasport.identity.domain.RegisterPerson
import br.com.locasport.identity.domain.RevokeRole
import br.com.locasport.identity.domain.Role
import br.com.locasport.identity.domain.StreamId
import br.com.locasport.identity.domain.SubmitPartnerForReview
import br.com.locasport.identity.domain.SuspendPerson
import br.com.locasport.identity.domain.VerifyPartnerIdentity
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
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
        val personProjection = PersonProjection(client)
        val partnerProjection = PartnerProjection(client)
        val assuranceProjection = IdentityAssuranceProjection(client)
        val compositeProjection =
            CompositeProjectionStore(listOf(personProjection, partnerProjection, assuranceProjection))
        val referenceProjection = ComplianceReferenceProjection(client)

        val registerPerson = RegisterPersonHandler(deduplication, eventStore, compositeProjection, publisher)
        val disclosePurpose = DisclosePersonPurposeHandler(deduplication, eventStore, compositeProjection, publisher)
        val restrictivePolicy = DefaultLevelUpgradePolicy()
        val permissivePolicy =
            object : LevelUpgradePolicy {
                override suspend fun mayUpgradeTo(
                    subjectId: java.util.UUID,
                    target: AssuranceLevel,
                ) = true
            }
        val raiseAssurance =
            RaisePersonAssuranceLevelHandler(
                deduplication,
                eventStore,
                compositeProjection,
                publisher,
                restrictivePolicy,
            )
        val raisePartnerAssurance =
            RaisePartnerAssuranceLevelHandler(
                deduplication,
                eventStore,
                compositeProjection,
                publisher,
                permissivePolicy,
            )
        val activateAccount = ActivatePersonAccountHandler(deduplication, eventStore, compositeProjection, publisher)
        val grantRole = GrantPersonRoleHandler(deduplication, eventStore, compositeProjection, publisher)
        val revokeRole = RevokePersonRoleHandler(deduplication, eventStore, compositeProjection, publisher)
        val suspendPerson = SuspendPersonHandler(deduplication, eventStore, compositeProjection, publisher)
        val reactivatePerson = ReactivatePersonHandler(deduplication, eventStore, compositeProjection, publisher)
        val disclosePartnerPurpose =
            DisclosePartnerPurposeHandler(deduplication, eventStore, compositeProjection, publisher)
        val registerPartner = RegisterPartnerHandler(deduplication, eventStore, compositeProjection, publisher)
        val submitReview = SubmitPartnerForReviewHandler(deduplication, eventStore, compositeProjection, publisher)
        val verifyIdentity = VerifyPartnerIdentityHandler(deduplication, eventStore, compositeProjection, publisher)
        val aclConsumer = ComplianceAclConsumer(sqs, properties, referenceProjection)

        afterSpec {
            sqs.close()
            sns.close()
        }

        test("descarta comando duplicado e persiste apenas um evento sob carga concorrente") {
            val personId = PersonId.random()
            val command = RegisterPerson(CommandId(UUID.randomUUID()), personId, LegalBasis.CONTRACT_EXECUTION)

            val outcomes =
                coroutineScope {
                    (1..50).map { async(Dispatchers.IO) { registerPerson.handle(command) } }.awaitAll()
                }

            outcomes.count { it is CommandOutcome.Applied } shouldBe 1
            countEvents(client, personId.value) shouldBe 1L
        }

        test("preserva ordem FIFO dos eventos do stream Person na SQS") {
            val personId = PersonId.random()
            registerPerson.handle(RegisterPerson(CommandId(UUID.randomUUID()), personId, LegalBasis.CONTRACT_EXECUTION))
            disclosePurpose.handle(DisclosePurpose(CommandId(UUID.randomUUID()), personId, "treino"))
            raiseAssurance.handle(
                RaiseAssuranceLevel(CommandId(UUID.randomUUID()), personId.value, AssuranceLevel.BASIC),
            )

            eventStore.load(StreamId.of(personId)).map { it.event.type } shouldBe
                listOf("PersonRegistered", "PurposeDisclosed", "AssuranceLevelRaised")

            drainTypesForStream(sqs, eventsQueueUrl, personId.value.toString(), mapper, expected = 3) shouldBe
                listOf("PersonRegistered", "PurposeDisclosed", "AssuranceLevelRaised")
        }

        test("rejeita ativacao sem assurance BASIC (REGISTERED -> ACTIVATE)") {
            val personId = PersonId.random()
            registerPerson.handle(RegisterPerson(CommandId(UUID.randomUUID()), personId, LegalBasis.CONTRACT_EXECUTION))

            shouldThrow<AccountActivationDenied> {
                activateAccount.handle(ActivateAccount(CommandId(UUID.randomUUID()), personId))
            }
        }

        test("rejeita downgrade de assurance") {
            val personId = PersonId.random()
            registerPerson.handle(RegisterPerson(CommandId(UUID.randomUUID()), personId, LegalBasis.CONTRACT_EXECUTION))
            disclosePurpose.handle(DisclosePurpose(CommandId(UUID.randomUUID()), personId, "treino"))
            raiseAssurance.handle(
                RaiseAssuranceLevel(CommandId(UUID.randomUUID()), personId.value, AssuranceLevel.BASIC),
            )

            shouldThrow<InvalidAssuranceDowngrade> {
                raiseAssurance.handle(
                    RaiseAssuranceLevel(CommandId(UUID.randomUUID()), personId.value, AssuranceLevel.NONE),
                )
            }
        }

        test("rejeita coleta excessiva VERIFIED sem gatilho de risco") {
            val personId = PersonId.random()
            registerPerson.handle(RegisterPerson(CommandId(UUID.randomUUID()), personId, LegalBasis.CONTRACT_EXECUTION))
            disclosePurpose.handle(DisclosePurpose(CommandId(UUID.randomUUID()), personId, "treino"))
            raiseAssurance.handle(
                RaiseAssuranceLevel(CommandId(UUID.randomUUID()), personId.value, AssuranceLevel.BASIC),
            )

            shouldThrow<ExcessiveAssuranceDenied> {
                raiseAssurance.handle(
                    RaiseAssuranceLevel(CommandId(UUID.randomUUID()), personId.value, AssuranceLevel.VERIFIED),
                )
            }
        }

        test("rejeita RaiseAssurance sem proposito divulgado") {
            val personId = PersonId.random()
            registerPerson.handle(RegisterPerson(CommandId(UUID.randomUUID()), personId, LegalBasis.CONTRACT_EXECUTION))

            shouldThrow<PurposeNotDisclosed> {
                raiseAssurance.handle(
                    RaiseAssuranceLevel(CommandId(UUID.randomUUID()), personId.value, AssuranceLevel.BASIC),
                )
            }
        }

        test("fluxo completo Person: Register -> Disclose -> Raise -> Activate") {
            val personId = PersonId.random()
            registerPerson.handle(RegisterPerson(CommandId(UUID.randomUUID()), personId, LegalBasis.CONTRACT_EXECUTION))
            disclosePurpose.handle(DisclosePurpose(CommandId(UUID.randomUUID()), personId, "treino"))
            raiseAssurance.handle(
                RaiseAssuranceLevel(CommandId(UUID.randomUUID()), personId.value, AssuranceLevel.BASIC),
            )
            activateAccount.handle(ActivateAccount(CommandId(UUID.randomUUID()), personId))

            eventStore.load(StreamId.of(personId)).map { it.event.type } shouldBe
                listOf("PersonRegistered", "PurposeDisclosed", "AssuranceLevelRaised", "PersonActivated")

            loadPersonStatus(client, personId.value) shouldBe "ACTIVE"
        }

        test("fluxo completo Partner: Register -> Review -> Raise -> VerifyIdentity") {
            val partnerId = PartnerId.random()
            val legalName = "Arena Teste Ltda"
            val taxId = "11222333000144"
            val verifiedAt = Instant.now()

            registerPartner.handle(
                RegisterPartner(
                    CommandId(UUID.randomUUID()),
                    partnerId,
                    PartnerType.ARENA,
                    legalName,
                    taxId,
                    LegalBasis.CONTRACT_EXECUTION,
                ),
            )
            submitReview.handle(SubmitPartnerForReview(CommandId(UUID.randomUUID()), partnerId))

            eventStore.load(StreamId.of(partnerId)).map { it.event.type } shouldBe
                listOf("PartnerRegistered", "PartnerReviewSubmitted")

            disclosePartnerPurpose.handle(
                DisclosePartnerPurpose(CommandId(UUID.randomUUID()), partnerId, "arena"),
            )
            raisePartnerAssurance.handle(
                RaiseAssuranceLevel(CommandId(UUID.randomUUID()), partnerId.value, AssuranceLevel.VERIFIED),
            )
            verifyIdentity.handle(VerifyPartnerIdentity(CommandId(UUID.randomUUID()), partnerId, verifiedAt))

            eventStore.load(StreamId.of(partnerId)).map { it.event.type } shouldBe
                listOf("PartnerRegistered", "PartnerReviewSubmitted", "PurposeDisclosed", "AssuranceLevelRaised", "PartnerIdentityVerified")

            loadPartnerStatus(client, partnerId.value) shouldBe "ACTIVE"
        }

        test("revoga papel atribuido e remove da projecao") {
            val personId = PersonId.random()
            registerPerson.handle(RegisterPerson(CommandId(UUID.randomUUID()), personId, LegalBasis.CONTRACT_EXECUTION))
            disclosePurpose.handle(DisclosePurpose(CommandId(UUID.randomUUID()), personId, "treino"))
            raiseAssurance.handle(
                RaiseAssuranceLevel(CommandId(UUID.randomUUID()), personId.value, AssuranceLevel.BASIC),
            )
            activateAccount.handle(ActivateAccount(CommandId(UUID.randomUUID()), personId))
            grantRole.handle(GrantRole(CommandId(UUID.randomUUID()), personId, Role.PRACTITIONER))
            revokeRole.handle(RevokeRole(CommandId(UUID.randomUUID()), personId, Role.PRACTITIONER))

            eventStore.load(StreamId.of(personId)).map { it.event.type } shouldBe
                listOf(
                    "PersonRegistered",
                    "PurposeDisclosed",
                    "AssuranceLevelRaised",
                    "PersonActivated",
                    "RoleGranted",
                    "RoleRevoked",
                )

            loadPersonRoles(client, personId.value) shouldBe ""
        }

        test("suspende e reativa pessoa fim-a-fim") {
            val personId = PersonId.random()
            registerPerson.handle(RegisterPerson(CommandId(UUID.randomUUID()), personId, LegalBasis.CONTRACT_EXECUTION))
            disclosePurpose.handle(DisclosePurpose(CommandId(UUID.randomUUID()), personId, "treino"))
            raiseAssurance.handle(
                RaiseAssuranceLevel(CommandId(UUID.randomUUID()), personId.value, AssuranceLevel.BASIC),
            )
            activateAccount.handle(ActivateAccount(CommandId(UUID.randomUUID()), personId))
            suspendPerson.handle(SuspendPerson(CommandId(UUID.randomUUID()), personId, "abuso"))
            reactivatePerson.handle(ReactivatePerson(CommandId(UUID.randomUUID()), personId))

            eventStore.load(StreamId.of(personId)).map { it.event.type } shouldBe
                listOf(
                    "PersonRegistered",
                    "PurposeDisclosed",
                    "AssuranceLevelRaised",
                    "PersonActivated",
                    "PersonSuspended",
                    "PersonReactivated",
                )

            loadPersonStatus(client, personId.value) shouldBe "ACTIVE"
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

private suspend fun loadPersonRoles(
    client: DatabaseClient,
    personId: UUID,
): String? =
    client
        .sql("SELECT roles FROM person_view WHERE person_id = :id")
        .bind("id", personId)
        .map { row -> row.get("roles", String::class.java) ?: "" }
        .awaitOneOrNull()

private suspend fun loadPersonStatus(
    client: DatabaseClient,
    personId: UUID,
): String? =
    client
        .sql("SELECT status FROM person_view WHERE person_id = :id")
        .bind("id", personId)
        .map { row -> row.get("status", String::class.java)!! }
        .awaitOneOrNull()

private suspend fun loadPartnerStatus(
    client: DatabaseClient,
    partnerId: UUID,
): String? =
    client
        .sql("SELECT status FROM partner_view WHERE partner_id = :id")
        .bind("id", partnerId)
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
