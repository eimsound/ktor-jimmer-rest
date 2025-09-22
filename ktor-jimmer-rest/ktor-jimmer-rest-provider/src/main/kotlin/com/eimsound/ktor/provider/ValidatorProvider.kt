package com.eimsound.ktor.provider

import com.eimsound.ktor.provider.Validators.Entity
import com.eimsound.ktor.provider.Validators.InputType
import com.eimsound.ktor.validator.ValidationBuilder
import com.eimsound.ktor.validator.ValidationResult
import com.eimsound.ktor.validator.validate
import io.ktor.http.HttpStatusCode
import org.babyfish.jimmer.Input

//@DslMarker
//annotation class ValidatorDslMarker

sealed class Validators<T> {
    data class Entity<T : Any>(val validate: ValidatorScope.(T) -> Unit) : Validators<T>()
    data class InputType<T : Any, out TInput : Input<T>>(
        val validate: ValidatorScope.(@UnsafeVariance TInput) -> Unit
    ) : Validators<T>()
}

fun <T : Any> Validators<T>?.validate(body: T) = this?.run {
    if (this is Entity<T>)
        ValidatorScope().validate(body, validate).`throw`()
}

inline fun <reified T : Any, reified TInput : Input<T>> Validators<T>?.validate(
    body: TInput
) = this?.run {
    if (this is InputType<T, *>)
        ValidatorScope().validate(body, validate).`throw`()
}

fun ValidationResult.`throw`() = let {
    if (it is ValidationResult.Invalid) {
        it.`throw`(HttpStatusCode.BadRequest)
    }
}

interface ValidatorProvider<T> {
    var validator: Validators<T>?
}

@InputDslMarker
class ValidatorScope : ValidationBuilder()

fun <T : Any> EntityScope<T>.validator(block: ValidatorScope.(T) -> Unit) {
    validator = Entity(block)
}

fun <T : Any, TInput : Input<T>> InputScope<T, TInput>.validator(block: ValidatorScope.(TInput) -> Unit) {
    validator = InputType(block)
}

