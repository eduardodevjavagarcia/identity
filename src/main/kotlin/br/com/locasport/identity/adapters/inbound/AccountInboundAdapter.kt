package br.com.locasport.identity.adapters.inbound

import br.com.locasport.identity.application.AssignRoleHandler
import br.com.locasport.identity.application.RaiseAssuranceLevelHandler
import br.com.locasport.identity.application.RegisterAccountHandler
import br.com.locasport.identity.application.ReinstateAccountHandler
import br.com.locasport.identity.application.RevokeRoleHandler
import br.com.locasport.identity.application.SubmitIdentityClaimHandler
import br.com.locasport.identity.application.SuspendAccountHandler
import br.com.locasport.identity.domain.CommandId
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import java.util.UUID

class AccountInboundAdapter(
    private val register: RegisterAccountHandler,
    private val submitClaim: SubmitIdentityClaimHandler,
    private val raiseAssurance: RaiseAssuranceLevelHandler,
    private val assignRole: AssignRoleHandler,
    private val suspendAccount: SuspendAccountHandler,
    private val revokeRole: RevokeRoleHandler,
    private val reinstate: ReinstateAccountHandler,
) {
    suspend fun register(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<RegisterAccountRequest>()
        return ServerResponse.ok().bodyValueAndAwait(register.handle(payload.toCommand(commandId)))
    }

    suspend fun submitClaim(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<SubmitIdentityClaimRequest>()
        return ServerResponse.ok().bodyValueAndAwait(submitClaim.handle(payload.toCommand(commandId)))
    }

    suspend fun raiseAssurance(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<RaiseAssuranceLevelRequest>()
        return ServerResponse.ok().bodyValueAndAwait(raiseAssurance.handle(payload.toCommand(commandId)))
    }

    suspend fun assignRole(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<AssignRoleRequest>()
        return ServerResponse.ok().bodyValueAndAwait(assignRole.handle(payload.toCommand(commandId)))
    }

    suspend fun suspendAccount(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<SuspendAccountRequest>()
        return ServerResponse.ok().bodyValueAndAwait(suspendAccount.handle(payload.toCommand(commandId)))
    }

    suspend fun revokeRole(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<RevokeRoleRequest>()
        return ServerResponse.ok().bodyValueAndAwait(revokeRole.handle(payload.toCommand(commandId)))
    }

    suspend fun reinstate(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<ReinstateAccountRequest>()
        return ServerResponse.ok().bodyValueAndAwait(reinstate.handle(payload.toCommand(commandId)))
    }

    private fun commandId(request: ServerRequest): CommandId = CommandId(UUID.fromString(request.headers().firstHeader(COMMAND_ID_HEADER)))

    private companion object {
        const val COMMAND_ID_HEADER = "X-Command-Id"
    }
}
