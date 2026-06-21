package br.com.locasport.identity.adapters.inbound

import br.com.locasport.identity.application.DeactivatePartnerHandler
import br.com.locasport.identity.application.DisclosePartnerPurposeHandler
import br.com.locasport.identity.application.GrantPartnerRoleHandler
import br.com.locasport.identity.application.RaisePartnerAssuranceLevelHandler
import br.com.locasport.identity.application.ReactivatePartnerHandler
import br.com.locasport.identity.application.RegisterPartnerHandler
import br.com.locasport.identity.application.RejectPartnerHandler
import br.com.locasport.identity.application.RevokePartnerRoleHandler
import br.com.locasport.identity.application.SubmitPartnerForReviewHandler
import br.com.locasport.identity.application.SuspendPartnerHandler
import br.com.locasport.identity.application.VerifyPartnerIdentityHandler
import br.com.locasport.identity.domain.CommandId
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import java.util.UUID

class PartnerInboundAdapter(
    private val register: RegisterPartnerHandler,
    private val submitForReview: SubmitPartnerForReviewHandler,
    private val verifyIdentity: VerifyPartnerIdentityHandler,
    private val reject: RejectPartnerHandler,
    private val raiseAssurance: RaisePartnerAssuranceLevelHandler,
    private val disclosePurpose: DisclosePartnerPurposeHandler,
    private val grantRole: GrantPartnerRoleHandler,
    private val revokeRole: RevokePartnerRoleHandler,
    private val suspend: SuspendPartnerHandler,
    private val reactivate: ReactivatePartnerHandler,
    private val deactivate: DeactivatePartnerHandler,
) {
    suspend fun register(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<RegisterPartnerRequest>()
        return ServerResponse.ok().bodyValueAndAwait(register.handle(payload.toCommand(commandId)))
    }

    suspend fun submitForReview(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<SubmitPartnerForReviewRequest>()
        return ServerResponse.ok().bodyValueAndAwait(submitForReview.handle(payload.toCommand(commandId)))
    }

    suspend fun verifyIdentity(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<VerifyPartnerIdentityRequest>()
        return ServerResponse.ok().bodyValueAndAwait(verifyIdentity.handle(payload.toCommand(commandId)))
    }

    suspend fun reject(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<RejectPartnerRequest>()
        return ServerResponse.ok().bodyValueAndAwait(reject.handle(payload.toCommand(commandId)))
    }

    suspend fun raiseAssurance(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<RaiseAssuranceLevelRequest>()
        return ServerResponse.ok().bodyValueAndAwait(raiseAssurance.handle(payload.toCommand(commandId)))
    }

    suspend fun disclosePurpose(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<DisclosePartnerPurposeRequest>()
        return ServerResponse.ok().bodyValueAndAwait(disclosePurpose.handle(payload.toCommand(commandId)))
    }

    suspend fun grantRole(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<GrantPartnerRoleRequest>()
        return ServerResponse.ok().bodyValueAndAwait(grantRole.handle(payload.toCommand(commandId)))
    }

    suspend fun revokeRole(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<RevokePartnerRoleRequest>()
        return ServerResponse.ok().bodyValueAndAwait(revokeRole.handle(payload.toCommand(commandId)))
    }

    suspend fun suspend(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<SuspendPartnerRequest>()
        return ServerResponse.ok().bodyValueAndAwait(suspend.handle(payload.toCommand(commandId)))
    }

    suspend fun reactivate(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<ReactivatePartnerRequest>()
        return ServerResponse.ok().bodyValueAndAwait(reactivate.handle(payload.toCommand(commandId)))
    }

    suspend fun deactivate(request: ServerRequest): ServerResponse {
        val commandId = commandId(request)
        val payload = request.awaitBody<DeactivatePartnerRequest>()
        return ServerResponse.ok().bodyValueAndAwait(deactivate.handle(payload.toCommand(commandId)))
    }

    private fun commandId(request: ServerRequest): CommandId = CommandId(UUID.fromString(request.headers().firstHeader(COMMAND_ID_HEADER)))

    private companion object {
        const val COMMAND_ID_HEADER = "X-Command-Id"
    }
}
