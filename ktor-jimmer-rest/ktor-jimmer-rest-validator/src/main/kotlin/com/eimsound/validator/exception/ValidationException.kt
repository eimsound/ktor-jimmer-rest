package com.eimsound.ktor.jimmer.rest.validator.exception

import io.ktor.http.HttpStatusCode

class ValidationException(
    val httpStatusCode: HttpStatusCode,
    val errors: List<String>
) : IllegalArgumentException()
