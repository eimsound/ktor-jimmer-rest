package com.eimsound.ktor.provider

import com.eimsound.ktor.validator.ValidationBuilder
import org.babyfish.jimmer.Input
import kotlin.reflect.KClass

@DslMarker
annotation class InputDslMarker

sealed class Inputs<T> {
    object Entity : Inputs<Any>()
    data class InputEntity<T : Any>(
        val inputType: KClass<out Input<T>>
    ) : Inputs<T>()
}

@InputDslMarker
interface InputProvider<T : Any> : ValidatorProvider, TransformProvider<T> {
    var input: Inputs<T>
}

@InputDslMarker
class InputScope<T : Any, TInput : Input<T>>(
    var validator: Validators.InputEntity<T, TInput>? = null,
) {
    fun validator(block: ValidationBuilder.(TInput) -> Unit) {
        validator = Validators.InputEntity<T, TInput>(block)
    }
}

@InputDslMarker
class EntityScope<T : Any>(
    var validator: Validators.Entity<T>? = null,
) {
    fun validator(block: ValidationBuilder.(T) -> Unit) {
        validator = Validators.Entity<T>(block)
    }
}

fun <T : Any> InputProvider<T>.input(
    block: EntityScope<T>.() -> Unit
) = run {
    val scope = EntityScope<T>().apply { block() }
    input = Inputs.Entity as Inputs<T>
    validator = scope.validator
    EntityTransformBuilder<T>(this)
}

fun <T : Any, TInput : Input<T>> InputProvider<T>.input(
    type: KClass<TInput>,
    block: InputScope<T, TInput>.() -> Unit
) = run {
    val scope = InputScope<T, TInput>().apply { block() }
    input = Inputs.InputEntity<T>(type)
    validator = scope.validator
    InputEntityTransformBuilder<T, TInput>(this)
}
