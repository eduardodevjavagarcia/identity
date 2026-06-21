package br.com.locasport.identity.adapters.inbound

import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.coRouter

fun identityRoutes(
    person: PersonInboundAdapter,
    partner: PartnerInboundAdapter,
): RouterFunction<ServerResponse> =
    coRouter {
        "/persons".nest {
            POST("", person::register)
            POST("/purpose", person::disclosePurpose)
            POST("/assurance", person::raiseAssurance)
            POST("/activate", person::activateAccount)
            POST("/roles", person::grantRole)
            POST("/roles/revoke", person::revokeRole)
            POST("/suspend", person::suspend)
            POST("/reactivate", person::reactivate)
            POST("/deactivate", person::deactivate)
        }
        "/partners".nest {
            POST("", partner::register)
            POST("/submit-review", partner::submitForReview)
            POST("/verify-identity", partner::verifyIdentity)
            POST("/reject", partner::reject)
            POST("/assurance", partner::raiseAssurance)
            POST("/purpose", partner::disclosePurpose)
            POST("/roles", partner::grantRole)
            POST("/roles/revoke", partner::revokeRole)
            POST("/suspend", partner::suspend)
            POST("/reactivate", partner::reactivate)
            POST("/deactivate", partner::deactivate)
        }
    }
