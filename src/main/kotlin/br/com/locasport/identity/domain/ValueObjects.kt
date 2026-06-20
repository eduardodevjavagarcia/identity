package br.com.locasport.identity.domain

enum class SubjectType {
    PERSON,
    PARTNER,
}

enum class Role {
    PRACTITIONER,
    PARTNER_ARENA,
    PARTNER_INSTRUCTOR,
    ADMIN,
}

enum class AssuranceLevel {
    NONE,
    LOW,
    SUBSTANTIAL,
    HIGH,
}

enum class FactorType {
    PASSWORD,
    OTP,
    DEVICE,
}

enum class ClaimType {
    EMAIL,
    PHONE,
    GOVERNMENT_ID,
    SELFIE,
    ADDRESS,
}

@JvmInline
value class PurposeReference(
    val value: String,
) {
    companion object {
        fun none(): PurposeReference = PurposeReference("")
    }
}

@JvmInline
value class LegalBasisReference(
    val value: String,
) {
    companion object {
        fun none(): LegalBasisReference = LegalBasisReference("")
    }
}
