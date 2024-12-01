package com.eimsound.ktor.jimmer.rest.provider

import com.eimsound.ktor.jimmer.rest.validator.ValidationBuilder


interface ValidatorProvider<T> {
    var validator: (ValidationBuilder.(T) -> Unit)?
}

fun <T> ValidatorProvider<T>.validate(block: ValidationBuilder.(T) -> Unit) {
    validator = block
}
