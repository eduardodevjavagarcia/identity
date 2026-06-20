package br.com.locasport.identity.adapters.inbound

import br.com.locasport.identity.application.ActivateCredentialHandler
import br.com.locasport.identity.application.CompleteStepUpChallengeHandler
import br.com.locasport.identity.application.RegisterCredentialHandler
import br.com.locasport.identity.application.RevokeCredentialHandler
import br.com.locasport.identity.domain.CommandId
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import java.util.UUID

class CredentialInboundAdapter(
    private val register: RegisterCredentialHandler,
    private val activate: ActivateCredentialHandler,
    private val completeStepUp: CompleteStepUpChallengeHandler,
    private val revoke: RevokeCredentialHandler,
) {
    suspend fun register(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<RegisterCredentialRequest>()
        return ServerResponse.ok().bodyValueAndAwait(register.handle(payload.toCommand(commandId)))
    }

    suspend fun activate(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<ActivateCredentialRequest>()
        return ServerResponse.ok().bodyValueAndAwait(activate.handle(payload.toCommand(commandId)))
    }

    suspend fun completeStepUp(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<CompleteStepUpChallengeRequest>()
        return ServerResponse.ok().bodyValueAndAwait(completeStepUp.handle(payload.toCommand(commandId)))
    }

    suspend fun revoke(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<RevokeCredentialRequest>()
        return ServerResponse.ok().bodyValueAndAwait(revoke.handle(payload.toCommand(commandId)))
    }

    private fun commandId(request: ServerRequest): CommandId = CommandId(UUID.fromString(request.headers().firstHeader(COMMAND_ID_HEADER)))

    private companion object {
        const val COMMAND_ID_HEADER = "X-Command-Id"
    }
}
