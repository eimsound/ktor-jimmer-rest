package com.eimsound.validator

import com.eimsound.validator.exception.ValidationException
import io.ktor.http.HttpStatusCode

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult() {
        fun `throw`(httpStatusCode: HttpStatusCode) {
            throw ValidationException(httpStatusCode, errors)
        }
    }
}
