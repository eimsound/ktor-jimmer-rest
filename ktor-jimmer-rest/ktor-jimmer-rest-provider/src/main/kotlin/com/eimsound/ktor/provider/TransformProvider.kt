package com.eimsound.ktor.provider

import org.babyfish.jimmer.Input

sealed class Transformers<T : Any> {
    class Entity<T : Any>(val transformer: (T) -> T) : Transformers<T>()
    class InputEntity<T : Any, TInput : Input<T>>(val transformer: (TInput) -> TInput) : Transformers<T>()


    inline fun transform(body: T): T {
        return (this as Entity<T>).transformer?.invoke(body) ?: body
    }

    inline fun <TInput : Input<T>> transform(body: TInput): TInput {
        return (this as InputEntity<T, TInput>).transformer?.invoke(body) ?: body
    }
}

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
