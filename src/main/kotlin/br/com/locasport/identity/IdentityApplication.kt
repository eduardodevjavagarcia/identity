package br.com.locasport.identity

import br.com.locasport.identity.adapters.config.IdentityPropertiesInitializer
import br.com.locasport.identity.adapters.config.identityBeans
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IdentityApplication

fun main(args: Array<String>) {
    runApplication<IdentityApplication>(*args) {
        addInitializers(IdentityPropertiesInitializer(), identityBeans)
    }
}
