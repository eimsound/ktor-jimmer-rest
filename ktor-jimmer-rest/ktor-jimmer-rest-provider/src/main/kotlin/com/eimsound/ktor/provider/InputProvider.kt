package com.eimsound.ktor.provider

import com.eimsound.ktor.validator.ValidationBuilder
import org.babyfish.jimmer.Input
import kotlin.reflect.KClass

@DslMarker
annotation class InputDslMarker

sealed class Inputs<T> {
    class Entity<T> : Inputs<T>()
    data class InputType<T : Any>(
        val inputType: KClass<out Input<T>>
    ) : Inputs<T>()
}

@InputDslMarker
interface InputProvider<T : Any> : ValidatorProvider<T>, TransformProvider<T> {
    var input: Inputs<T>
}

@InputDslMarker
class InputScope<T : Any, TInput : Input<T>>(
    var validator: Validators.InputType<T, TInput>? = null,
    var transformer: Transformers.InputType<T, TInput>? = null
)

@InputDslMarker
class EntityScope<T : Any>(
    var validator: Validators.Entity<T>? = null,
    var transformer: Transformers.Entity<T>? = null
)

inline fun <T : Any> InputProvider<T>.input(
    block: EntityScope<T>.() -> Unit
) {
    val scope = EntityScope<T>().apply { block() }
    input = Inputs.Entity()
    validator = scope.validator
    transformer = scope.transformer
}

inline fun <T : Any, TInput : Input<T>> InputProvider<T>.input(
    type: KClass<TInput>,
    block: InputScope<T, TInput>.() -> Unit
) {
    val scope = InputScope<T, TInput>().apply { block() }
    input = Inputs.InputType(type)
    validator = scope.validator
    transformer = scope.transformer
}
