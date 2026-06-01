package de.openkleinanzeigen.core.domain

class ApiException(
    message: String,
    val httpCode: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
