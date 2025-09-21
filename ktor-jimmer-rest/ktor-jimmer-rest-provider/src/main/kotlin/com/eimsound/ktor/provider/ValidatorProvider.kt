package com.eimsound.ktor.provider

import com.eimsound.ktor.provider.Validators.Entity
import com.eimsound.ktor.provider.Validators.InputType
import com.eimsound.ktor.validator.ValidationBuilder
import com.eimsound.ktor.validator.ValidationResult
import com.eimsound.ktor.validator.validate
import io.ktor.http.HttpStatusCode
import org.babyfish.jimmer.Input

sealed class Validators<T> {
    data class Entity<T : Any>(val validate: ValidationBuilder.(T) -> Unit) : Validators<T>()
    data class InputType<T : Any, out TInput : Input<T>>(
        val validate: ValidationBuilder.(@UnsafeVariance TInput) -> Unit
    ) : Validators<T>()
}

inline fun <T : Any> Validators<T>?.validate(body: T) = this?.run {
    if (this is Entity<T>) validate(body, validate).`throw`()
}

inline fun <reified T : Any, reified TInput : Input<T>> Validators<T>?.validate(
    body: TInput
) = this?.run {
    if (this is InputType<T, *>)
        validate(body, validate).`throw`()
}

inline fun ValidationResult.`throw`() = let {
    if (it is ValidationResult.Invalid) {
        it.`throw`(HttpStatusCode.BadRequest)
    }
}

interface ValidatorProvider<T> {
    var validator: Validators<T>?
}

//fun <T : Any> ValidatorProvider<T>.validator(block: ValidationBuilder.(T) -> Unit) {
//    validator = Validators.Entity(block)
//}
//
//@JvmName("validatorInput")
//fun <TInput : Input<*>> ValidatorProvider<*>.validator(block: ValidationBuilder.(TInput) -> Unit) {
//    validator = Validators.InputEntity(block)
//}
//
