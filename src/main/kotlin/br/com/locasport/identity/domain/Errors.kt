package br.com.locasport.identity.domain

class InvalidStateTransition(
    val from: Enum<*>,
    val to: Enum<*>,
) : RuntimeException("invalid transition $from -> $to")

class InvalidAssuranceDowngrade(
    val from: AssuranceLevel,
    val to: AssuranceLevel,
) : RuntimeException("assurance downgrade $from -> $to")
