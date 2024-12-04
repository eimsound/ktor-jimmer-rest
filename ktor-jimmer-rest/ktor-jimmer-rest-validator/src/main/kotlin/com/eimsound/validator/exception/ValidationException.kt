package com.eimsound.validator.exception

import io.ktor.http.HttpStatusCode

class ValidationException(
    val httpStatusCode: HttpStatusCode,
    val errors: List<String>
) : IllegalArgumentException()
