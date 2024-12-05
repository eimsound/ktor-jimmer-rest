package com.eimsound.ktor.jimmer.rest.validator

import com.eimsound.ktor.jimmer.rest.validator.exception.ValidationException
import io.ktor.http.HttpStatusCode

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult() {
        fun `throw`(httpStatusCode: HttpStatusCode) {
            throw ValidationException(httpStatusCode, errors)
        }
    }
}
