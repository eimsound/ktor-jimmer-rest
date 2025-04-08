package com.eimsound.ktor.provider

import com.eimsound.ktor.validator.ValidationBuilder
import com.eimsound.ktor.validator.ValidationResult
import com.eimsound.ktor.validator.validate
import io.ktor.http.HttpStatusCode
import org.babyfish.jimmer.Input

sealed class Validators {
    data class Entity<T : Any>(val validate: ValidationBuilder.(T) -> Unit) : Validators()
    data class InputEntity<TInput : Input<*>>(val validate: ValidationBuilder.(TInput) -> Unit) :
        Validators()

    operator inline fun <T : Any> invoke(body: T) {
        validate(body, (this as Entity<T>).validate).`throw`()
    }

    operator inline fun <T : Any, TInput : Input<T>> invoke(body: TInput) {
        validate(body, (this as InputEntity<TInput>).validate).`throw`()
    }

    inline fun ValidationResult.`throw`() = let {
        if (it is ValidationResult.Invalid) {
            it.`throw`(HttpStatusCode.BadRequest)
        }
    }
}

interface ValidatorProvider<T : Any> {
    var validator: Validators?
}

fun <T : Any> ValidatorProvider<T>.validator(block: ValidationBuilder.(T) -> Unit) {
    validator = Validators.Entity(block)
}
//
//@JvmName("validatorInput")
//fun <TInput : Input<*>> ValidatorProvider<*>.validator(block: ValidationBuilder.(TInput) -> Unit) {
//    validator = Validators.InputEntity(block)
//}
//
