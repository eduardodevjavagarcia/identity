package br.com.locasport.identity.domain

class InvalidStateTransition(
    val from: Enum<*>,
    val to: Enum<*>,
) : RuntimeException("invalid transition $from -> $to")

class InvalidAssuranceDowngrade(
    val from: AssuranceLevel,
    val to: AssuranceLevel,
) : RuntimeException("assurance downgrade $from -> $to")

class AccountActivationDenied(
    val personId: PersonId,
    val assurance: AssuranceLevel,
) : RuntimeException("activation denied for $personId with assurance $assurance")

class PartnerApprovalDenied(
    val partnerId: PartnerId,
    val assurance: AssuranceLevel,
) : RuntimeException("approval denied for $partnerId with assurance $assurance")

class ExcessiveAssuranceDenied(
    val subjectId: Any,
    val target: AssuranceLevel,
) : RuntimeException("excessive assurance $target denied for $subjectId")

class PurposeNotDisclosed(
    val subjectId: Any,
) : RuntimeException("purpose not disclosed for $subjectId")
