package com.eimsound.ktor.provider

import com.eimsound.ktor.provider.Transformers.Entity
import com.eimsound.ktor.provider.Transformers.InputEntity
import org.babyfish.jimmer.Input
import kotlin.reflect.KClass

sealed class Transformers<T : Any> {
    data class Entity<T : Any>(val transformer: (T) -> T) : Transformers<T>()
    data class InputEntity<T : Any, TInput : Input<T>>(
        val transformer: (TInput) -> TInput
    ) : Transformers<T>()
}

inline fun <T : Any> Transformers<T>?.transform(body: T): T = when (this) {
    is Entity<T> -> transformer(body)
    else -> body
}

inline fun <T : Any, reified TInput : Input<T>> Transformers<T>?.transform(
    inputType: KClass<out TInput>,
    body: TInput
): TInput = when (this) {
    is InputEntity<T, *> -> {
        if (inputType == TInput::class)
            @Suppress("UNCHECKED_CAST") (this as InputEntity<T, TInput>).transformer(body)
        else body
    }

    else -> body
} as TInput

interface TransformProvider<T : Any> {
    var transformer: Transformers<T>?
}

class EntityTransformBuilder<T : Any>(var provider: TransformProvider<T>)
class InputEntityTransformBuilder<T : Any, TInput : Input<T>>(var provider: TransformProvider<T>)

fun <T : Any> EntityTransformBuilder<T>.transform(block: (T) -> T) {
    provider.transformer = Transformers.Entity(block)
}

fun <T : Any, TInput : Input<T>> InputEntityTransformBuilder<T, TInput>.transform(block: (TInput) -> TInput) {
    provider.transformer = Transformers.InputEntity(block)
}
