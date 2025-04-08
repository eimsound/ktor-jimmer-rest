package com.eimsound.ktor.provider

import com.eimsound.ktor.validator.ValidationBuilder
import org.babyfish.jimmer.Input
import kotlin.reflect.KClass

sealed class Inputs<T> {
    object Entity : Inputs<Any>()
    data class InputEntity<T>(val inputType: KClass<out Input<T>>) : Inputs<T>()
}

interface InputProvider<T : Any> : ValidatorProvider<T> {
    var input: Inputs<T>
}


@FilterDslMarker
class InputScope<T : Any, TInput : Input<T>>(var validator: (ValidationBuilder.(TInput) -> Unit)? = null) {
    fun validator(block: ValidationBuilder.(TInput) -> Unit) {
        validator = block
    }
}

fun <T : Any, TInput : Input<T>> InputProvider<T>.input(
    type: KClass<TInput>,
    block: InputScope<T, TInput>.() -> Unit
) = apply {
    input = Inputs.InputEntity(type)
    validator = Validators.InputEntity<TInput>(InputScope<T, TInput>().apply { block() }.validator ?: {})
}
