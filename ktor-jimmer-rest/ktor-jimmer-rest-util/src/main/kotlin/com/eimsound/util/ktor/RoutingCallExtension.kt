package com.eimsound.util.ktor

import com.eimsound.util.reflect.getPropertyReceiver
import io.ktor.server.routing.RoutingCall
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import com.eimsound.ktor.config.Configuration
import com.eimsound.util.parser.parse
import com.eimsound.util.jimmer.tableName
import com.eimsound.util.jimmer.tableType

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

/**
 *
 * Returns the first parameter with a value if any.
 *
 * @receiver ExtParameterMap<T>
 * @return Parameter<T>?
 */
inline fun <T> ExtParameterMap<T>.default() = values.firstOrNull()


/**
 * Returns the default value from the map if available.
 *
 * @receiver ExtParameterMap<T>
 * @return T? The default value or null if no value is found.
 */
inline fun <T> ExtParameterMap<T>.defaultValue() = values.firstOrNull()?.value

/**
 *
 * Given a KProperty, this function filters the query parameters
 * to find ones that ends with the property name, and splits them by the given ext.
 * It returns a map of parameter name and optional extension.
 *
 * @receiver RoutingCall
 * @param property KProperty<KExpression<T>>
 * @param ext String?
 * @return ExtParameterMap<T>
 */
inline fun <reified T : Any> RoutingCall.queryParameterExt(
    property: KProperty<KExpression<T>>,
    ext: String? = null
): ExtParameterMap<T> {
    val receiver = getPropertyReceiver(property)
    val tableName = tableName(receiver)
    val tableType = tableType(receiver)
    val type = Class.forName(tableType).kotlin
    val name = (tableName.split(".") + property.name).drop(1)
        .joinToString(Configuration.subParameterSeparator)
    return queryParameterExt(type, name, ext)
}

/**
 * Extension function for RoutingCall to find query parameters with a specific separator and extension.
 *
 * Given a KClass, a base name and a separator, this function filters the query parameters
 * to find ones that end with the given name, and splits them by the separator.
 * It returns a map of parameter name and optional extension.
 *
 * @receiver RoutingCall
 * @param type KClass<*>
 * @param name String
 * @param ext String?
 * @return ExtParameterMap<T>
 */
inline fun <reified T : Any> RoutingCall.queryParameterExt(
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

/**
 * This extension function for `RoutingCall` provides an overload of `queryParameterExt`
 * that allows querying parameters by their `name` and optional `ext` without specifying a `KClass` type.
 *
 * The function internally calls the `queryParameterExt` that requires a `KClass`, using the reified type `T`.
 *
 * This is useful for simplifying calls to `queryParameterExt` when the type can be inferred.
 *
 * @receiver RoutingCall
 * @param name String
 * @param ext String?
 * @return ExtParameterMap<T>
 */
inline fun <reified T : Any> RoutingCall.queryParameterExt(
    name: String,
    ext: String? = null
): ExtParameterMap<T> = queryParameterExt(T::class, name, ext)


/**
 * This extension function for `RoutingCall` retrieves a query parameter by its `name`
 * and attempts to parse it into the specified type `T`.
 *
 * The function uses the provided `KClass` to perform type-safe parsing of the query
 * parameter value found in the `queryParameters` map.
 *
 * If the parameter is not present or cannot be parsed into the specified type, the
 * function returns null.
 *
 * Usage of this function requires an explicit type to be provided for parsing.
 *
 * @receiver RoutingCall
 * @param type KClass<*>
 * @param name String
 * @return T? The parsed query parameter value or null if not present or unparsable.
 */
inline fun <T : Any> RoutingCall.queryParameter(type: KClass<*>, name: String): T? {
    val serializable = queryParameters[name]?.parse(type)
    return serializable as T?
}

/**
 * This extension function for `RoutingCall` retrieves a query parameter by its `name`
 * and attempts to parse it into the specified type `T`.
 *
 * The function uses the provided `KClass` to perform type-safe parsing of the query
 * parameter value found in the `queryParameters` map.
 *
 * If the parameter is not present or cannot be parsed into the specified type, the
 * function returns null.
 *
 * Usage of this function requires an explicit type to be provided for parsing.
 *
 * @receiver RoutingCall
 * @param name String
 * @return T?
 */
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
