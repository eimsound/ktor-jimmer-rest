package com.eimsound.ktor.provider

import com.eimsound.ktor.provider.Validators.Entity
import com.eimsound.ktor.provider.Validators.InputEntity
import com.eimsound.ktor.validator.ValidationBuilder
import com.eimsound.ktor.validator.ValidationResult
import com.eimsound.ktor.validator.validate
import io.ktor.http.HttpStatusCode
import org.babyfish.jimmer.Input
import kotlin.reflect.KClass

sealed class Validators<T> {
    data class Entity<T : Any>(val validate: ValidationBuilder.(T) -> Unit) : Validators<T>()
    data class InputEntity<T : Any, TInput : Input<T>>(val validate: ValidationBuilder.(TInput) -> Unit) :
        Validators<T>()
}

inline fun <T : Any> Validators<T>?.validate(body: T) = this?.run {
    if (this is Entity<T>) validate(body, validate).`throw`()
}

inline fun <reified T : Any, reified TInput : Input<T>> Validators<T>?.validate(
    inputType: KClass<out TInput>,
    body: TInput
) = this?.run {
    if (inputType == TInput::class && this is InputEntity<T, *>)
        validate(body, @Suppress("UNCHECKED_CAST") (this as InputEntity<T, TInput>).validate).`throw`()
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
