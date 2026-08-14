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
    check(this is Entity<T>) {
        "validator 配置与实体入参不匹配：配置了 ${this::class.simpleName}，但入参是实体。" +
            "请检查 input {} 与 validator {} 的配置是否一致。"
    }
    ValidatorScope().validate(body, validate).`throw`()
}

inline fun <reified T : Any, reified TInput : Input<T>> Validators<T>?.validate(
    body: TInput
) = this?.run {
    check(this is InputType<T, *>) {
        "validator 配置与 Input 入参不匹配：配置了 ${this::class.simpleName}，但入参是 Input<...>。" +
            "请检查 input(InputType::class) {} 与 validator {} 的配置是否一致。"
    }
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
