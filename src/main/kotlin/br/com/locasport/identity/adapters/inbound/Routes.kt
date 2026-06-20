package br.com.locasport.identity.adapters.inbound

import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.coRouter

fun identityRoutes(
    account: AccountInboundAdapter,
    credential: CredentialInboundAdapter,
): RouterFunction<ServerResponse> =
    coRouter {
        "/accounts".nest {
            POST("", account::register)
            POST("/claims", account::submitClaim)
            POST("/assurance", account::raiseAssurance)
            POST("/roles", account::assignRole)
            POST("/roles/revoke", account::revokeRole)
            POST("/suspend", account::suspendAccount)
            POST("/reinstate", account::reinstate)
        }
        "/credentials".nest {
            POST("", credential::register)
            POST("/activate", credential::activate)
            POST("/step-up", credential::completeStepUp)
            POST("/revoke", credential::revoke)
        }
    }
