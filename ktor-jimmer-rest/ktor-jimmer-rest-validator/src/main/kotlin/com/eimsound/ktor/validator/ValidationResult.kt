package com.eimsound.ktor.validator

import com.eimsound.ktor.validator.exception.ValidationException
import io.ktor.http.HttpStatusCode

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult() {
        inline fun `throw`(httpStatusCode: HttpStatusCode) {
            throw ValidationException(httpStatusCode, errors)
        }
    }
}
