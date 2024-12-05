package com.eimsound.ktor.jimmer.rest.provider

import com.eimsound.ktor.jimmer.rest.util.parser.parse
import io.ktor.server.routing.*
import kotlin.reflect.KClass
import com.eimsound.ktor.jimmer.rest.config.Configuration

interface CallProvider {
    val call: RoutingCall
}


inline fun <T : Any> RoutingCall.param(type: KClass<*>, name: String): T? {
    val serializable = queryParameters[name]?.parse(type)
    return serializable as T?
}

inline fun <reified T : Any> RoutingCall.param(name: String): T? {
    return param(T::class, name)
}

//inline fun <reified T : Any, reified E : Any> RoutingCall.param(parameter: Parameter<E>): E? {
//    val type = getTypeByPropertyName<T, E>(parameter)
//    return param(type, parameter.nameWithExt) as E?
//}

val RoutingCall.defaultPathVariable
    get() = pathParameters[pathParameters.names().first()]
        ?: throw IllegalStateException("path variable not found")


/**
 * Create a condition according to the HTTP parameter.
 *
 * The HTTP parameter name is the property name of the entity,
 * and the HTTP parameter value is the value of the condition.
 * The condition is `eq` by default.
 * If the HTTP parameter name is suffixed with a special character,
 * that special character is used as the operator of the condition.
 * The supported special characters are `gt`, `lt`, `ge`, `le`.
 * For example, if the HTTP parameter is `name_gt=abc`,
 * the condition is `name gt 'abc'`.
 *
 * @param out T
 * @property kProperty0 KProperty0<T>
 * @property ext String?
 * @property separator String
 * @property nameWithExt String
 * @constructor
 */
class Parameter<T>(val name: String) {
    // 比如 ge le exact
    var ext: String? = null
    val separator get() = Configuration.parameterSeparator
    val hasExt get() = ext != null
    // 比如 createTime__ge createTime__le name__exact
    val nameWithExt: String get() = name + if (hasExt) separator + ext else ""

    var value: T? = null
}
