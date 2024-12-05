package com.eimsound.ktor.provider

import com.eimsound.ktor.validator.ValidationBuilder
import com.eimsound.ktor.validator.ValidationResult
import com.eimsound.ktor.validator.validate
import io.ktor.http.HttpStatusCode


interface ValidatorProvider<T> {
    var validator: (ValidationBuilder.(T) -> Unit)?
}

fun <T> ValidatorProvider<T>.validator(block: ValidationBuilder.(T) -> Unit) {
    validator = block
}

inline fun <reified T : Any> (ValidationBuilder.(T) -> Unit).validate(body: T) {
    validate(body, this).let {
        if (it is ValidationResult.Invalid) {
            it.`throw`(HttpStatusCode.BadRequest)
        }
    }
}
