package com.eimsound.ktor.provider

import com.eimsound.ktor.provider.Transformers.Entity
import com.eimsound.ktor.provider.Transformers.InputType
import org.babyfish.jimmer.Input

//@DslMarker
//annotation class TransformerDslMarker

sealed class Transformers<T : Any> {
    data class Entity<T : Any>(val transformer: TransformerScope.(T) -> T) : Transformers<T>()
    data class InputType<T : Any, out TInput : Input<T>>(
        val transformer: TransformerScope.(@UnsafeVariance TInput) -> TInput
    ) : Transformers<T>()
}

fun <T : Any> Transformers<T>?.transform(
    body: T
): T = if (this is Entity<T>) TransformerScope().transformer(body) else body

inline fun <T : Any, reified TInput : Input<T>> Transformers<T>?.transform(
    body: TInput
) = if (this is InputType<T, *>) TransformerScope().transformer(body) else body


//@TransformerDslMarker
interface TransformProvider<T : Any> {
    var transformer: Transformers<T>?
}

@InputDslMarker
class TransformerScope

fun <T : Any> EntityScope<T>.transformer(block: TransformerScope.(T) -> T) {
    transformer = Entity(block)
}

fun <T : Any, TInput : Input<T>> InputScope<T,TInput>.transformer(
    block: TransformerScope.(TInput) -> TInput
) {
    transformer = InputType(block)
}
