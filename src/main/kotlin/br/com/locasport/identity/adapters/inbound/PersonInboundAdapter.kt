package br.com.locasport.identity.adapters.inbound

import br.com.locasport.identity.application.ActivatePersonAccountHandler
import br.com.locasport.identity.application.DeactivatePersonHandler
import br.com.locasport.identity.application.DisclosePersonPurposeHandler
import br.com.locasport.identity.application.GrantPersonRoleHandler
import br.com.locasport.identity.application.RaisePersonAssuranceLevelHandler
import br.com.locasport.identity.application.ReactivatePersonHandler
import br.com.locasport.identity.application.RegisterPersonHandler
import br.com.locasport.identity.application.RevokePersonRoleHandler
import br.com.locasport.identity.application.SuspendPersonHandler
import br.com.locasport.identity.domain.CommandId
import br.com.locasport.identity.domain.RaiseAssuranceLevel
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import java.util.UUID

class PersonInboundAdapter(
    private val register: RegisterPersonHandler,
    private val disclosePurpose: DisclosePersonPurposeHandler,
    private val raiseAssurance: RaisePersonAssuranceLevelHandler,
    private val activateAccount: ActivatePersonAccountHandler,
    private val grantRole: GrantPersonRoleHandler,
    private val revokeRole: RevokePersonRoleHandler,
    private val suspend: SuspendPersonHandler,
    private val reactivate: ReactivatePersonHandler,
    private val deactivate: DeactivatePersonHandler,
) {
    suspend fun register(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<RegisterPersonRequest>()
        return ServerResponse.ok().bodyValueAndAwait(register.handle(payload.toCommand(commandId)))
    }

    suspend fun disclosePurpose(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<DisclosePurposeRequest>()
        return ServerResponse.ok().bodyValueAndAwait(disclosePurpose.handle(payload.toCommand(commandId)))
    }

    suspend fun raiseAssurance(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<RaiseAssuranceLevelRequest>()
        val command = RaiseAssuranceLevel(commandId, payload.subjectId, payload.target)
        return ServerResponse.ok().bodyValueAndAwait(raiseAssurance.handle(command))
    }

    suspend fun activateAccount(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<ActivateAccountRequest>()
        return ServerResponse.ok().bodyValueAndAwait(activateAccount.handle(payload.toCommand(commandId)))
    }

    suspend fun grantRole(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<GrantRoleRequest>()
        return ServerResponse.ok().bodyValueAndAwait(grantRole.handle(payload.toCommand(commandId)))
    }

    suspend fun revokeRole(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<RevokeRoleRequest>()
        return ServerResponse.ok().bodyValueAndAwait(revokeRole.handle(payload.toCommand(commandId)))
    }

    suspend fun suspend(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<SuspendPersonRequest>()
        return ServerResponse.ok().bodyValueAndAwait(suspend.handle(payload.toCommand(commandId)))
    }

    suspend fun reactivate(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<ReactivatePersonRequest>()
        return ServerResponse.ok().bodyValueAndAwait(reactivate.handle(payload.toCommand(commandId)))
    }

    suspend fun deactivate(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<DeactivatePersonRequest>()
        return ServerResponse.ok().bodyValueAndAwait(deactivate.handle(payload.toCommand(commandId)))
    }

    private fun commandId(request: ServerRequest): CommandId = CommandId(UUID.fromString(request.headers().firstHeader(COMMAND_ID_HEADER)))

    private companion object {
        const val COMMAND_ID_HEADER = "X-Command-Id"
    }
}
