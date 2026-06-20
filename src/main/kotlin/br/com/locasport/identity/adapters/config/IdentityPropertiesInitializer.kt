package br.com.locasport.identity.adapters.config

import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.registerBean

class IdentityPropertiesInitializer : ApplicationContextInitializer<GenericApplicationContext> {
    override fun initialize(context: GenericApplicationContext) {
        val properties =
            Binder
                .get(context.environment)
                .bind("identity", IdentityProperties::class.java)
                .get()
        context.registerBean { properties }
    }
}
