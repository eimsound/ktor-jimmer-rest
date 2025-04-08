package com.eimsound.ktor.provider


import com.eimsound.util.ktor.default
import com.eimsound.util.ktor.queryParameterExt
import com.eimsound.util.parser.parse
import com.fasterxml.jackson.annotation.JsonProperty
import org.babyfish.jimmer.sql.ast.LikeMode
import org.babyfish.jimmer.sql.kt.ast.expression.*
import org.babyfish.jimmer.sql.kt.ast.query.specification.KSpecification
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.primaryConstructor

inline fun <reified S : KSpecification<*>> FilterScope<*>.specification(): S {
    val constructor = S::class.primaryConstructor
        ?: throw IllegalArgumentException("No primary constructor found for ${S::class.simpleName}")

    val parameters = constructor.parameters.map {
        val propertyName = it.annotations.first { it is JsonProperty } as JsonProperty
        val queryParameter =
            call.queryParameters[propertyName.value]?.parse(
                it.type.classifier as KClass<*>
            )
        it to queryParameter
    }.toMap()
    val instance = constructor.callBy(parameters)
    return instance
}

inline fun <reified T : Any, reified P : Any> FilterScope<T>.`eq?`(param: KProperty<KExpression<P>>)
    : KNonNullExpression<Boolean>? {
    val parameter = call.queryParameterExt<P>(param).default()
    return param?.call()?.`eq?`(parameter?.value)
}


@Suppress("ktlint:standard:function-naming")
inline fun <reified T : Any> FilterScope<T>.`ilike?`(
    param: KProperty<KExpression<String>>,
): KNonNullExpression<Boolean>? {
    val parameter =  call.queryParameterExt<String>(param).default()
    val likeMode = when (parameter?.ext) {
        "anywhere" -> LikeMode.ANYWHERE
        "exact" -> LikeMode.EXACT
        "start" -> LikeMode.START
        "end" -> LikeMode.END
        else -> LikeMode.ANYWHERE
    }
    return param?.call()?.`ilike?`(parameter?.value, likeMode)
}

inline fun <reified T : Any, reified P : Comparable<*>> FilterScope<T>.`between?`(
    param: KProperty<KExpression<P>>,
): KNonNullExpression<Boolean>? {
    val parameter = call.queryParameterExt<P>(param)
    return param?.call()?.`between?`(parameter["ge"]?.value, parameter["le"]?.value)
}


inline fun <reified T : Any, reified P : Any> FilterScope<T>.noNull(param: KProperty<KExpression<P>>)
    : KNonNullExpression<Boolean>? {
    return param?.call()?.isNotNull()
}
