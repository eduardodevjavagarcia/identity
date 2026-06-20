package br.com.locasport.identity.adapters.config

import br.com.locasport.identity.adapters.inbound.AccountInboundAdapter
import br.com.locasport.identity.adapters.inbound.ComplianceAclConsumer
import br.com.locasport.identity.adapters.inbound.CredentialInboundAdapter
import br.com.locasport.identity.adapters.inbound.identityRoutes
import br.com.locasport.identity.adapters.outbound.AccountProjection
import br.com.locasport.identity.adapters.outbound.AvroEventCodec
import br.com.locasport.identity.adapters.outbound.ComplianceReferenceProjection
import br.com.locasport.identity.adapters.outbound.CredentialProjection
import br.com.locasport.identity.adapters.outbound.PostgresCommandDeduplication
import br.com.locasport.identity.adapters.outbound.R2dbcEventStore
import br.com.locasport.identity.adapters.outbound.S3RetentionArchive
import br.com.locasport.identity.adapters.outbound.SqsFifoEventPublisher
import br.com.locasport.identity.application.ActivateCredentialHandler
import br.com.locasport.identity.application.AssignRoleHandler
import br.com.locasport.identity.application.CompleteStepUpChallengeHandler
import br.com.locasport.identity.application.RaiseAssuranceLevelHandler
import br.com.locasport.identity.application.RegisterAccountHandler
import br.com.locasport.identity.application.RegisterCredentialHandler
import br.com.locasport.identity.application.ReinstateAccountHandler
import br.com.locasport.identity.application.RevokeCredentialHandler
import br.com.locasport.identity.application.RevokeRoleHandler
import br.com.locasport.identity.application.StepUpAssurancePolicy
import br.com.locasport.identity.application.SubmitIdentityClaimHandler
import br.com.locasport.identity.application.SuspendAccountHandler
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
        bean { AccountProjection(ref()) }
        bean { CredentialProjection(ref()) }
        bean { RegisterAccountHandler(ref(), ref(), ref<AccountProjection>(), ref()) }
        bean { SubmitIdentityClaimHandler(ref(), ref(), ref<AccountProjection>(), ref()) }
        bean { RaiseAssuranceLevelHandler(ref(), ref(), ref<AccountProjection>(), ref()) }
        bean { AssignRoleHandler(ref(), ref(), ref<AccountProjection>(), ref()) }
        bean { SuspendAccountHandler(ref(), ref(), ref<AccountProjection>(), ref()) }
        bean { RevokeRoleHandler(ref(), ref(), ref<AccountProjection>(), ref()) }
        bean { ReinstateAccountHandler(ref(), ref(), ref<AccountProjection>(), ref()) }
        bean { RegisterCredentialHandler(ref(), ref(), ref<CredentialProjection>(), ref()) }
        bean { ActivateCredentialHandler(ref(), ref(), ref<CredentialProjection>(), ref()) }
        bean { CompleteStepUpChallengeHandler(ref(), ref(), ref<CredentialProjection>(), ref()) }
        bean { RevokeCredentialHandler(ref(), ref(), ref<CredentialProjection>(), ref()) }
        bean { StepUpAssurancePolicy(ref()) }
        bean { AccountInboundAdapter(ref(), ref(), ref(), ref(), ref(), ref(), ref()) }
        bean { CredentialInboundAdapter(ref(), ref(), ref(), ref()) }
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
