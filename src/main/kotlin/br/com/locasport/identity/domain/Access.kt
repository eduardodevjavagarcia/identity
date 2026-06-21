package br.com.locasport.identity.domain

data class Access(
    val credentialType: CredentialType,
    val active: Boolean,
)

enum class CredentialType {
    PASSWORD,
    BIOMETRIC,
}
