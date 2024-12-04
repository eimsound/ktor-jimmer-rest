package com.eimsound.ktor.jimmer.rest.provider

import com.eimsound.ktor.jimmer.rest.validator.ValidationBuilder
import com.eimsound.ktor.jimmer.rest.validator.validate
import com.eimsound.validator.ValidationResult
import io.ktor.http.HttpStatusCode


interface ValidatorProvider<T> {
    var validator: (ValidationBuilder.(T) -> Unit)?
}

fun <T> ValidatorProvider<T>.validator(block: ValidationBuilder.(T) -> Unit) {
    validator = block
}

fun <T : Any> (ValidationBuilder.(T) -> Unit).validate(body: T) {
    validate(body, this).let {
        if (it is ValidationResult.Invalid) {
            it.`throw`(HttpStatusCode.BadRequest)
        }
    }
}
