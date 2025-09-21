package com.eimsound.ktor.provider

import com.eimsound.ktor.provider.Transformers.Entity
import com.eimsound.ktor.provider.Transformers.InputType
import org.babyfish.jimmer.Input

sealed class Transformers<T : Any> {
    data class Entity<T : Any>(val transformer: (T) -> T) : Transformers<T>()
    data class InputType<T : Any, out TInput : Input<T>>(
        val transformer: (@UnsafeVariance TInput) -> TInput
    ) : Transformers<T>()
}

interface TransformProvider<T : Any> {
    var transformer: Transformers<T>?
}

inline fun <T : Any> Transformers<T>?.transform(body: T): T =
    if (this is Entity<T>) this.transformer(body) else body

inline fun <T : Any, reified TInput : Input<T>> Transformers<T>?.transform(
    body: TInput
) =
    if (this is InputType<T, *>) this.transformer(body) else body
