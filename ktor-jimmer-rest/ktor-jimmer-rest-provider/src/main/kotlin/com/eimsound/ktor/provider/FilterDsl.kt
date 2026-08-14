package com.eimsound.ktor.provider

import com.eimsound.util.ktor.default
import com.eimsound.ktor.config.Configuration
import com.eimsound.util.ktor.queryParameter
import com.eimsound.util.ktor.queryParameterValues
import com.eimsound.util.ktor.queryParameterExtMap
import com.eimsound.util.ktor.queryParameterExtValue
import com.eimsound.util.ktor.ResolvedName
import io.ktor.http.parsing.ParseException
import org.babyfish.jimmer.sql.ast.LikeMode
import org.babyfish.jimmer.sql.kt.ast.expression.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * 相等匹配。参数解析规则（确定性，与参数顺序无关）：
 * 优先无后缀参数（`?name=x`），其次 `__exact`（`?name__exact=x`），其他 ext 忽略。
 */
@PublishedApi
internal fun <P : Any> FilterQueryScope<*>.eqCore(
    type: KClass<P>,
    resolved: ResolvedName,
    expr: KExpression<P>,
): KNonNullExpression<Boolean>? {
    val parameters = call.queryParameterExtMap(type, resolved.value)
    val value = parameters[null]?.value ?: parameters["exact"]?.value
    return expr.`eq?`(value)
}

inline fun <reified T : Any, reified P : Any> FilterQueryScope<T>.`eq?`(param: KProperty<KExpression<P>>)
    : KNonNullExpression<Boolean>? = eqCore(P::class, resolved(param), param.call())

inline fun <reified T : Any, reified P : Any> FilterQueryScope<T>.`eq?`(param: KPropExpression<P>)
    : KNonNullExpression<Boolean>? = eqCore(P::class, resolved(param), param)

/**
 * 不相等匹配，读无后缀参数（`?name=x`）。
 * 注意：与 [eq?] 使用同一参数名，同一属性不要同时使用。
 */
@PublishedApi
internal fun <P : Any> FilterQueryScope<*>.notEqCore(
    type: KClass<P>,
    resolved: ResolvedName,
    expr: KExpression<P>,
): KNonNullExpression<Boolean>? {
    val value = call.queryParameter(type, resolved.value)
    return expr.`ne?`(value)
}

inline fun <reified T : Any, reified P : Any> FilterQueryScope<T>.`notEq?`(param: KProperty<KExpression<P>>)
    : KNonNullExpression<Boolean>? = notEqCore(P::class, resolved(param), param.call())

inline fun <reified T : Any, reified P : Any> FilterQueryScope<T>.`notEq?`(param: KPropExpression<P>)
    : KNonNullExpression<Boolean>? = notEqCore(P::class, resolved(param), param)

/**
 * 包含匹配，支持逗号分隔（`?id=1,2`）或重复参数（`?id=1&id=2`）；无值时不产生谓词。
 */
@PublishedApi
internal fun <P : Any> FilterQueryScope<*>.inCore(
    type: KClass<P>,
    resolved: ResolvedName,
    expr: KExpression<P>,
): KNonNullExpression<Boolean>? {
    val values = call.queryParameterValues(type, resolved.value)
    return if (values.isEmpty()) null else expr.valueIn(values)
}

inline fun <reified T : Any, reified P : Any> FilterQueryScope<T>.`in?`(param: KProperty<KExpression<P>>)
    : KNonNullExpression<Boolean>? = inCore(P::class, resolved(param), param.call())

inline fun <reified T : Any, reified P : Any> FilterQueryScope<T>.`in?`(param: KPropExpression<P>)
    : KNonNullExpression<Boolean>? = inCore(P::class, resolved(param), param)

/**
 * 不包含匹配，取值方式同 [in?]；无值时不产生谓词。
 */
@PublishedApi
internal fun <P : Any> FilterQueryScope<*>.notInCore(
    type: KClass<P>,
    resolved: ResolvedName,
    expr: KExpression<P>,
): KNonNullExpression<Boolean>? {
    val values = call.queryParameterValues(type, resolved.value)
    return if (values.isEmpty()) null else expr.valueNotIn(values)
}

inline fun <reified T : Any, reified P : Any> FilterQueryScope<T>.`notIn?`(param: KProperty<KExpression<P>>)
    : KNonNullExpression<Boolean>? = notInCore(P::class, resolved(param), param.call())

inline fun <reified T : Any, reified P : Any> FilterQueryScope<T>.`notIn?`(param: KPropExpression<P>)
    : KNonNullExpression<Boolean>? = notInCore(P::class, resolved(param), param)

/**
 * 小于，读 `{name}__lt`。
 */
@PublishedApi
internal fun <P : Comparable<*>> FilterQueryScope<*>.ltCore(
    type: KClass<P>,
    resolved: ResolvedName,
    expr: KExpression<P>,
): KNonNullExpression<Boolean>? {
    val value = call.queryParameterExtMap(type, resolved.value)["lt"]?.value
    return expr.`lt?`(value)
}

inline fun <reified T : Any, reified P : Comparable<*>> FilterQueryScope<T>.`lt?`(
    param: KProperty<KExpression<P>>,
): KNonNullExpression<Boolean>? = ltCore(P::class, resolved(param), param.call())

inline fun <reified T : Any, reified P : Comparable<*>> FilterQueryScope<T>.`lt?`(
    param: KPropExpression<P>,
): KNonNullExpression<Boolean>? = ltCore(P::class, resolved(param), param)

/**
 * 大于，读 `{name}__gt`。
 */
@PublishedApi
internal fun <P : Comparable<*>> FilterQueryScope<*>.gtCore(
    type: KClass<P>,
    resolved: ResolvedName,
    expr: KExpression<P>,
): KNonNullExpression<Boolean>? {
    val value = call.queryParameterExtMap(type, resolved.value)["gt"]?.value
    return expr.`gt?`(value)
}

inline fun <reified T : Any, reified P : Comparable<*>> FilterQueryScope<T>.`gt?`(
    param: KProperty<KExpression<P>>,
): KNonNullExpression<Boolean>? = gtCore(P::class, resolved(param), param.call())

inline fun <reified T : Any, reified P : Comparable<*>> FilterQueryScope<T>.`gt?`(
    param: KPropExpression<P>,
): KNonNullExpression<Boolean>? = gtCore(P::class, resolved(param), param)

/**
 * 小于等于，读 `{name}__le`。注意：与 [between?] 的 `__le` 参数同名，同一属性不要同时使用。
 */
@PublishedApi
internal fun <P : Comparable<*>> FilterQueryScope<*>.leCore(
    type: KClass<P>,
    resolved: ResolvedName,
    expr: KExpression<P>,
): KNonNullExpression<Boolean>? {
    val value = call.queryParameterExtMap(type, resolved.value)["le"]?.value
    return expr.`le?`(value)
}

inline fun <reified T : Any, reified P : Comparable<*>> FilterQueryScope<T>.`le?`(
    param: KProperty<KExpression<P>>,
): KNonNullExpression<Boolean>? = leCore(P::class, resolved(param), param.call())

inline fun <reified T : Any, reified P : Comparable<*>> FilterQueryScope<T>.`le?`(
    param: KPropExpression<P>,
): KNonNullExpression<Boolean>? = leCore(P::class, resolved(param), param)

/**
 * 大于等于，读 `{name}__ge`。注意：与 [between?] 的 `__ge` 参数同名，同一属性不要同时使用。
 */
@PublishedApi
internal fun <P : Comparable<*>> FilterQueryScope<*>.geCore(
    type: KClass<P>,
    resolved: ResolvedName,
    expr: KExpression<P>,
): KNonNullExpression<Boolean>? {
    val value = call.queryParameterExtMap(type, resolved.value)["ge"]?.value
    return expr.`ge?`(value)
}

inline fun <reified T : Any, reified P : Comparable<*>> FilterQueryScope<T>.`ge?`(
    param: KProperty<KExpression<P>>,
): KNonNullExpression<Boolean>? = geCore(P::class, resolved(param), param.call())

inline fun <reified T : Any, reified P : Comparable<*>> FilterQueryScope<T>.`ge?`(
    param: KPropExpression<P>,
): KNonNullExpression<Boolean>? = geCore(P::class, resolved(param), param)

/**
 * 模糊匹配。参数 `{name}` 支持 ext 后缀：
 * `__anywhere`（默认）/ `__exact` / `__start` / `__end`。
 */
@PublishedApi
internal fun FilterQueryScope<*>.ilikeCore(
    resolved: ResolvedName,
    expr: KExpression<String>,
): KNonNullExpression<Boolean>? {
    val parameter = call.queryParameterExtMap(String::class, resolved.value).default()
    val likeMode = when (parameter?.ext) {
        "anywhere" -> LikeMode.ANYWHERE
        "exact" -> LikeMode.EXACT
        "start" -> LikeMode.START
        "end" -> LikeMode.END
        else -> LikeMode.ANYWHERE
    }
    return expr.`ilike?`(parameter?.value, likeMode)
}

inline fun <reified T : Any> FilterQueryScope<T>.`ilike?`(
    param: KProperty<KExpression<String>>,
): KNonNullExpression<Boolean>? = ilikeCore(resolved(param), param.call())

inline fun <reified T : Any> FilterQueryScope<T>.`ilike?`(
    param: KPropExpression<String>,
): KNonNullExpression<Boolean>? = ilikeCore(resolved(param), param)

/**
 * 区间匹配，读 `{name}__ge` / `{name}__le`。
 */
@PublishedApi
internal fun <P : Comparable<*>> FilterQueryScope<*>.betweenCore(
    type: KClass<P>,
    resolved: ResolvedName,
    expr: KExpression<P>,
): KNonNullExpression<Boolean>? {
    val parameter = call.queryParameterExtMap(type, resolved.value)
    return expr.`between?`(parameter["ge"]?.value, parameter["le"]?.value)
}

inline fun <reified T : Any, reified P : Comparable<*>> FilterQueryScope<T>.`between?`(
    param: KProperty<KExpression<P>>,
): KNonNullExpression<Boolean>? = betweenCore(P::class, resolved(param), param.call())

inline fun <reified T : Any, reified P : Comparable<*>> FilterQueryScope<T>.`between?`(
    param: KPropExpression<P>,
): KNonNullExpression<Boolean>? = betweenCore(P::class, resolved(param), param)

/**
 * 非空谓词（静态，不读参数）。
 */
inline fun <reified T : Any, reified P : Any> FilterQueryScope<T>.noNull(param: KProperty<KExpression<P>>)
    : KNonNullExpression<Boolean>? = param.call().isNotNull()

inline fun <reified T : Any, reified P : Any> FilterQueryScope<T>.noNull(param: KPropExpression<P>)
    : KNonNullExpression<Boolean>? = param.isNotNull()

/**
 * 为空谓词（静态，不读参数）。
 */
inline fun <reified T : Any, reified P : Any> FilterQueryScope<T>.isNull(param: KProperty<KExpression<P>>)
    : KNonNullExpression<Boolean>? = param.call().isNull()

inline fun <reified T : Any, reified P : Any> FilterQueryScope<T>.isNull(param: KPropExpression<P>)
    : KNonNullExpression<Boolean>? = param.isNull()

/**
 * 动态排序。支持多字段多方向：
 *
 * ```
 * ?sort=price,desc&sort=id,asc
 * ```
 *
 * 方向缺省为 `asc`。字段名经 Jimmer 元数据校验，非法字段/方向抛 [ParseException]（通常映射为 400）。
 *
 * @param parameterName 排序参数名，默认 `sort`
 */
inline fun <reified T : Any> FilterScope<T>.sort(parameterName: String = Configuration.endpoint.sortParameterName) {
    call.queryParameters.getAll(parameterName).orEmpty().forEach { item ->
        val parts = item.split(",")
        val propertyName = parts[0].trim()
        val direction = parts.getOrNull(1)?.trim()?.lowercase() ?: "asc"
        if (propertyName.isEmpty()) {
            throw ParseException("排序参数格式错误: '$item'，应为 '字段,asc|desc'")
        }
        val expression = try {
            table.get<Comparable<*>>(propertyName)
        } catch (e: Exception) {
            throw ParseException("排序字段不存在: '$propertyName'")
        }
        val order = when (direction) {
            "asc" -> expression.asc()
            "desc" -> expression.desc()
            else -> throw ParseException("不支持的排序方向: '$direction'，仅支持 asc/desc")
        }
        orderBy(order)
    }
}
