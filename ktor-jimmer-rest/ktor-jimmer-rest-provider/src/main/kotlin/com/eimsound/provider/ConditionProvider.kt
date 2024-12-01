package com.eimsound.ktor.jimmer.rest.provider

import com.eimsound.ktor.jimmer.rest.util.reflect.getMemberByMemberName
import com.eimsound.ktor.jimmer.rest.util.reflect.getPropertyByPropertyName
import com.eimsound.ktor.jimmer.rest.util.reflect.getPropertyReceiver
import com.eimsound.ktor.jimmer.rest.config.Configuration
import io.ktor.server.routing.*
import org.babyfish.jimmer.sql.ast.LikeMode
import org.babyfish.jimmer.sql.kt.ast.expression.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0


interface ConditionProvider<T : Any> {
    val call: RoutingCall
}

fun <T : Any> ConditionProvider<T>.findNameWithExt(name: String): List<Pair<String, String?>> {
    val names = call.queryParameters.names()
    val list = names.filter { it.startsWith(name) }.map {
        val parameter = it.split(Configuration.parameterSeparator)
        parameter.get(0) to parameter.getOrNull(1)
    }
    return list
}

//inline fun <reified T : Any, reified P : Any> ConditionProvider<T>.parameterMemoize(
//    property: KProperty0<KExpression<P>>,
//    ext: String? = null
//): Map<String?, Parameter<P>?> {
//    return parameter(property, ext).memoize()
//}

inline fun <reified T : Any, reified P : Any> ConditionProvider<T>.parameter(
    property: KProperty<KExpression<P>>,
    ext: String? = null
): Map<String?, Parameter<P>?> {
    val receiver = getPropertyReceiver(property)
    val javaTableName = getPropertyByPropertyName(receiver::class, "javaTable")?.getter?.call(receiver).toString()
    val typeName = getMemberByMemberName(receiver::class, "getImmutableType")?.call(receiver).toString()
    val type = Class.forName(typeName).kotlin
    val name = (javaTableName.split(".") + property.name).drop(1).joinToString(Configuration.subParameterSeparator)
    return parameter(type, name, ext)
}

inline fun <reified T : Any, reified P : Any> ConditionProvider<T>.parameter(
    type: KClass<*>,
    name: String,
    ext: String? = null
): Map<String?, Parameter<P>?> {
    val findNameWithExt = findNameWithExt(name)
    val map = findNameWithExt.map {
        val (parameterName, parameterExt) = it
        val parameter = Parameter<P>(parameterName).apply {
            this.ext = parameterExt ?: ext
            this.value = call.param(type, this.nameWithExt)
        }
        return@map parameter.ext to parameter
    }.toMap()
    return map
}


inline fun <reified T : Any, reified P : Any> ConditionProvider<T>.`eq?`(param: KProperty0<KExpression<P>>)
    : KNonNullExpression<Boolean>? {
    val parameter = parameter<T, P>(param).values.firstOrNull()
    return param?.invoke()?.`eq?`(parameter?.value)
}


@Suppress("ktlint:standard:function-naming")
inline fun <reified T : Any> ConditionProvider<T>.`ilike?`(
    param: KProperty0<KExpression<String>>
): KNonNullExpression<Boolean>? {
    val parameter = parameter<T, String>(param).values?.firstOrNull()
    val likeMode = when (parameter?.ext) {
        "anywhere" -> LikeMode.ANYWHERE
        "exact" -> LikeMode.EXACT
        "start" -> LikeMode.START
        "end" -> LikeMode.END
        else -> LikeMode.ANYWHERE
    }
    return param?.invoke()?.`ilike?`(parameter?.value, likeMode)
}

inline fun <reified T : Any, reified P : Comparable<*>> ConditionProvider<T>.`between?`(
    param: KProperty0<KExpression<P>>,
): KNonNullExpression<Boolean>? {
    val parameter = parameter<T, P>(param)
    return param?.invoke()?.`between?`(parameter["ge"]?.value, parameter["le"]?.value)
}


inline fun <reified T : Any, reified P : Any> ConditionProvider<T>.noNull(param: KProperty0<KExpression<P>>)
        : KNonNullExpression<Boolean>? {
    return param?.invoke()?.isNotNull()
}
