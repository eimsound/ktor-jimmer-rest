package com.eimsound.ktor.provider


import com.eimsound.util.ktor.default
import com.eimsound.util.ktor.ParameterNames
import com.eimsound.util.ktor.queryParameterExt
import org.babyfish.jimmer.sql.ast.LikeMode
import org.babyfish.jimmer.sql.kt.ast.expression.*
import kotlin.reflect.KProperty

inline fun <reified T : Any, reified P : Any> FilterScope<T>.`eq?`(param: KProperty<KExpression<P>>)
    : KNonNullExpression<Boolean>? {
    val resolved = ParameterNames.resolveWithPath(param)
    ParameterNames.ensureNoRootCollision(table, resolved)
    val parameter = call.queryParameterExt<P>(P::class, resolved.value).default()
    return param.call().`eq?`(parameter?.value)
}


inline fun <reified T : Any> FilterScope<T>.`ilike?`(
    param: KProperty<KExpression<String>>,
): KNonNullExpression<Boolean>? {
    val resolved = ParameterNames.resolveWithPath(param)
    ParameterNames.ensureNoRootCollision(table, resolved)
    val parameter = call.queryParameterExt<String>(String::class, resolved.value).default()
    val likeMode = when (parameter?.ext) {
        "anywhere" -> LikeMode.ANYWHERE
        "exact" -> LikeMode.EXACT
        "start" -> LikeMode.START
        "end" -> LikeMode.END
        else -> LikeMode.ANYWHERE
    }
    return param.call().`ilike?`(parameter?.value, likeMode)
}

inline fun <reified T : Any, reified P : Comparable<*>> FilterScope<T>.`between?`(
    param: KProperty<KExpression<P>>,
): KNonNullExpression<Boolean>? {
    val resolved = ParameterNames.resolveWithPath(param)
    ParameterNames.ensureNoRootCollision(table, resolved)
    val parameter = call.queryParameterExt<P>(P::class, resolved.value)
    return param.call().`between?`(parameter["ge"]?.value, parameter["le"]?.value)
}


inline fun <reified T : Any, reified P : Any> FilterScope<T>.noNull(param: KProperty<KExpression<P>>)
    : KNonNullExpression<Boolean>? {
    return param.call().isNotNull()
}
