package br.com.locasport.identity.domain

enum class AssuranceLevel {
    NONE,
    BASIC,
    VERIFIED,
}

enum class Role {
    PRACTITIONER,
    ARENA_OPERATOR,
    INSTRUCTOR,
    ADMIN,
}

enum class LegalBasis {
    CONTRACT_EXECUTION,
    FRAUD_PREVENTION,
    LEGAL_OBLIGATION,
}

enum class PersonStatus {
    REGISTERED,
    ACTIVE,
    SUSPENDED,
    DEACTIVATED,
}

enum class PartnerStatus {
    REGISTERED,
    UNDER_REVIEW,
    ACTIVE,
    REJECTED,
    SUSPENDED,
    DEACTIVATED,
}

enum class PartnerType {
    ARENA,
    INSTRUCTOR,
}
