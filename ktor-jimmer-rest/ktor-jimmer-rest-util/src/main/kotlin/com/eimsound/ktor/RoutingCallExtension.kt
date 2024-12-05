package com.eimsound.ktor.jimmer.rest.util.ktor

import com.eimsound.ktor.jimmer.rest.util.reflect.getPropertyReceiver
import io.ktor.server.routing.RoutingCall
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import com.eimsound.ktor.jimmer.rest.config.Configuration
import com.eimsound.ktor.jimmer.rest.util.parser.parse
import com.eimsound.ktor.jimmer.rest.util.reflect.jimmer.tableName
import com.eimsound.ktor.jimmer.rest.util.reflect.jimmer.tableType

typealias ExtParameterMap<T> = Map<String?, Parameter<T>?>

/**
 * Extension function for RoutingCall to find query parameters with a specific separator.
 *
 * Given a base name and a separator, this function filters the query parameters
 * to find ones that start with the given name, and splits them by the separator.
 * It returns a list of pairs, where each pair consists of the parameter name and
 * an optional extension.
 *
 * @param name The base name to filter the query parameters.
 * @param separator The separator used to split the parameter names.
 * @return A list of pairs of parameter name and optional extension.
 */
fun RoutingCall.findQueryParameterNameWithExt(name: String, separator: String): List<Pair<String, String?>> {
    val names = queryParameters.names()
    val list = names.filter { it.startsWith(name) }.map {
        val parameter = it.split(separator)
        parameter.get(0) to parameter.getOrNull(1)
    }
    return list
}


inline fun <reified T : Any> RoutingCall.queryParameter(
    property: KProperty<KExpression<T>>,
    ext: String? = null
): ExtParameterMap<T> {
    val receiver = getPropertyReceiver(property)
    val tableName = tableName(receiver)
    val tableType = tableType(receiver)
    val type = Class.forName(tableType).kotlin
    val name = (tableName.split(".") + property.name).drop(1)
        .joinToString(Configuration.subParameterSeparator)
    return queryParameter(type, name, ext)
}


inline fun <T> ExtParameterMap<T>.default() = values.firstOrNull()


inline fun <reified T : Any> RoutingCall.queryParameter(
    type: KClass<*>,
    name: String,
    ext: String? = null
): ExtParameterMap<T> {
    val nameWithExtList = findQueryParameterNameWithExt(name, Configuration.extParameterSeparator)
    val map = nameWithExtList.map {
        val (parameterName, parameterExt) = it
        val parameter = Parameter<T>(parameterName).apply {
            this.ext = ext ?: parameterExt
            this.value = queryParameter(type, this.nameWithExt)
        }
        parameter.ext to parameter
    }.toMap()
    return map
}


inline fun <T : Any> RoutingCall.queryParameter(type: KClass<*>, name: String): T? {
    val serializable = queryParameters[name]?.parse(type)
    return serializable as T?
}

inline fun <reified T : Any> RoutingCall.queryParameter(name: String): T? {
    return queryParameter(T::class, name)
}

/**
 * An extension property for RoutingCall to get the default path variable.
 *
 * This property retrieves the first path variable from the RoutingCall's path parameters.
 * Throws an IllegalStateException if no path variable is found.
 *
 * @return The default path variable as a String.
 * @throws IllegalStateException if no path variable is found.
 */
val RoutingCall.defaultPathVariable
    get() = pathParameters[pathParameters.names().first()]
        ?: throw IllegalStateException("path variable not found")
