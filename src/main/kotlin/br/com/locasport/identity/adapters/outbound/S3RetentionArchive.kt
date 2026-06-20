package br.com.locasport.identity.adapters.outbound

import br.com.locasport.identity.adapters.config.RetentionProperties
import br.com.locasport.identity.domain.ObjectStoragePort
import kotlinx.coroutines.future.await
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient

class S3RetentionArchive(
    private val s3: S3AsyncClient,
    private val properties: RetentionProperties,
) : ObjectStoragePort {
    override suspend fun archive(
        key: String,
        payload: ByteArray,
    ) {
        s3
            .putObject(
                { request ->
                    request.bucket(properties.archiveBucket)
                    request.key(key)
                },
                AsyncRequestBody.fromBytes(payload),
            ).await()
    }
}
