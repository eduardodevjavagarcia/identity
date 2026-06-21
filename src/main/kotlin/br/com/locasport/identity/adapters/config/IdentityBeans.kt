package br.com.locasport.identity.adapters.config

import br.com.locasport.identity.adapters.inbound.ComplianceAclConsumer
import br.com.locasport.identity.adapters.inbound.PartnerInboundAdapter
import br.com.locasport.identity.adapters.inbound.PersonInboundAdapter
import br.com.locasport.identity.adapters.inbound.identityRoutes
import br.com.locasport.identity.adapters.outbound.AvroEventCodec
import br.com.locasport.identity.adapters.outbound.ComplianceReferenceProjection
import br.com.locasport.identity.adapters.outbound.CompositeProjectionStore
import br.com.locasport.identity.adapters.outbound.IdentityAssuranceProjection
import br.com.locasport.identity.adapters.outbound.PartnerProjection
import br.com.locasport.identity.adapters.outbound.PersonProjection
import br.com.locasport.identity.adapters.outbound.PostgresCommandDeduplication
import br.com.locasport.identity.adapters.outbound.R2dbcEventStore
import br.com.locasport.identity.adapters.outbound.S3RetentionArchive
import br.com.locasport.identity.adapters.outbound.SqsFifoEventPublisher
import br.com.locasport.identity.application.ActivatePersonAccountHandler
import br.com.locasport.identity.application.DeactivatePartnerHandler
import br.com.locasport.identity.application.DeactivatePersonHandler
import br.com.locasport.identity.application.DefaultLevelUpgradePolicy
import br.com.locasport.identity.application.DisclosePartnerPurposeHandler
import br.com.locasport.identity.application.DisclosePersonPurposeHandler
import br.com.locasport.identity.application.GrantPartnerRoleHandler
import br.com.locasport.identity.application.GrantPersonRoleHandler
import br.com.locasport.identity.application.LevelUpgradePolicy
import br.com.locasport.identity.application.RaisePartnerAssuranceLevelHandler
import br.com.locasport.identity.application.RaisePersonAssuranceLevelHandler
import br.com.locasport.identity.application.ReactivatePartnerHandler
import br.com.locasport.identity.application.ReactivatePersonHandler
import br.com.locasport.identity.application.RegisterPartnerHandler
import br.com.locasport.identity.application.RegisterPersonHandler
import br.com.locasport.identity.application.RejectPartnerHandler
import br.com.locasport.identity.application.RevokePartnerRoleHandler
import br.com.locasport.identity.application.RevokePersonRoleHandler
import br.com.locasport.identity.application.SubmitPartnerForReviewHandler
import br.com.locasport.identity.application.SuspendPartnerHandler
import br.com.locasport.identity.application.SuspendPersonHandler
import br.com.locasport.identity.application.VerifyPartnerIdentityHandler
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.boot.ApplicationRunner
import org.springframework.context.support.beans
import org.springframework.r2dbc.core.DatabaseClient
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.net.URI

val identityBeans =
    beans {
        bean { DatabaseClient.create(ref<ConnectionFactory>()) }
        bean { AvroEventCodec() }
        bean { R2dbcEventStore(ref(), ref(), ref<IdentityProperties>().eventStore) }
        bean { PostgresCommandDeduplication(ref()) }
        bean {
            val localstack = ref<IdentityProperties>().localstack
            SqsAsyncClient
                .builder()
                .endpointOverride(URI.create(localstack.endpoint))
                .region(Region.of(localstack.region))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.accessKey, localstack.secretKey),
                    ),
                ).build()
        }
        bean {
            val localstack = ref<IdentityProperties>().localstack
            S3AsyncClient
                .builder()
                .endpointOverride(URI.create(localstack.endpoint))
                .region(Region.of(localstack.region))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.accessKey, localstack.secretKey),
                    ),
                ).forcePathStyle(true)
                .build()
        }
        bean { SqsFifoEventPublisher(ref(), ref<IdentityProperties>().localstack) }
        bean { S3RetentionArchive(ref(), ref<IdentityProperties>().retention) }
        bean { PersonProjection(ref()) }
        bean { PartnerProjection(ref()) }
        bean { IdentityAssuranceProjection(ref()) }
        bean {
            CompositeProjectionStore(
                listOf(ref<PersonProjection>(), ref<PartnerProjection>(), ref<IdentityAssuranceProjection>()),
            )
        }
        bean<LevelUpgradePolicy> { DefaultLevelUpgradePolicy() }
        bean { RegisterPersonHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref()) }
        bean { DisclosePersonPurposeHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref()) }
        bean { RaisePersonAssuranceLevelHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref(), ref()) }
        bean { ActivatePersonAccountHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref()) }
        bean { GrantPersonRoleHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref()) }
        bean { RevokePersonRoleHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref()) }
        bean { SuspendPersonHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref()) }
        bean { ReactivatePersonHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref()) }
        bean { DeactivatePersonHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref()) }
        bean { RegisterPartnerHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref()) }
        bean { DisclosePartnerPurposeHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref()) }
        bean { SubmitPartnerForReviewHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref()) }
        bean { VerifyPartnerIdentityHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref()) }
        bean { RejectPartnerHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref()) }
        bean { SuspendPartnerHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref()) }
        bean { ReactivatePartnerHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref()) }
        bean { DeactivatePartnerHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref()) }
        bean { GrantPartnerRoleHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref()) }
        bean { RevokePartnerRoleHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref()) }
        bean { RaisePartnerAssuranceLevelHandler(ref(), ref(), ref<CompositeProjectionStore>(), ref(), ref()) }
        bean {
            PersonInboundAdapter(
                ref(),
                ref(),
                ref(),
                ref(),
                ref(),
                ref(),
                ref(),
                ref(),
                ref(),
            )
        }
        bean {
            PartnerInboundAdapter(
                ref(),
                ref(),
                ref(),
                ref(),
                ref(),
                ref(),
                ref(),
                ref(),
                ref(),
                ref(),
                ref(),
            )
        }
        bean { identityRoutes(ref(), ref()) }
        bean { ComplianceReferenceProjection(ref()) }
        bean { ComplianceAclConsumer(ref(), ref<IdentityProperties>().localstack, ref()) }
        bean {
            val consumer = ref<ComplianceAclConsumer>()
            ApplicationRunner {
                CoroutineScope(Dispatchers.IO).launch { consumer.run() }
            }
        }
    }
