package com.eimsound.ktor.provider

import com.eimsound.util.ktor.default
import com.eimsound.ktor.config.Configuration
import com.eimsound.util.ktor.queryParameter
import com.eimsound.util.ktor.queryParameterValues
import com.eimsound.util.ktor.queryParameterExt
import io.ktor.http.parsing.ParseException
import org.babyfish.jimmer.sql.ast.LikeMode
import org.babyfish.jimmer.sql.kt.ast.expression.*
import kotlin.reflect.KProperty

/**
 * 相等匹配。参数解析规则（确定性，与参数顺序无关）：
 * 优先无后缀参数（`?name=x`），其次 `__exact`（`?name__exact=x`），其他 ext 忽略。
 */
inline fun <reified T : Any, reified P : Any> FilterQueryScope<T>.`eq?`(param: KProperty<KExpression<P>>)
    : KNonNullExpression<Boolean>? {
    val resolved = resolved(param)
    val parameters = call.queryParameterExt<P>(P::class, resolved.value)
    val value = parameters[null]?.value ?: parameters["exact"]?.value
    return param.call().`eq?`(value)
}

/**
 * 不相等匹配，读无后缀参数（`?name=x`）。
 * 注意：与 [eq?] 使用同一参数名，同一属性不要同时使用。
 */
inline fun <reified T : Any, reified P : Any> FilterQueryScope<T>.`notEq?`(param: KProperty<KExpression<P>>)
    : KNonNullExpression<Boolean>? {
    val resolved = resolved(param)
    val value = call.queryParameter<P>(P::class, resolved.value)
    return param.call().`ne?`(value)
}

/**
 * 包含匹配，支持逗号分隔（`?id=1,2`）或重复参数（`?id=1&id=2`）；无值时不产生谓词。
 */
inline fun <reified T : Any, reified P : Any> FilterQueryScope<T>.`in?`(param: KProperty<KExpression<P>>)
    : KNonNullExpression<Boolean>? {
    val resolved = resolved(param)
    val values = call.queryParameterValues<P>(P::class, resolved.value)
    return if (values.isEmpty()) {
        null
    } else {
        param.call().valueIn(values)
    }
}

/**
 * 不包含匹配，取值方式同 [in?]；无值时不产生谓词。
 */
inline fun <reified T : Any, reified P : Any> FilterQueryScope<T>.`notIn?`(param: KProperty<KExpression<P>>)
    : KNonNullExpression<Boolean>? {
    val resolved = resolved(param)
    val values = call.queryParameterValues<P>(P::class, resolved.value)
    return if (values.isEmpty()) {
        null
    } else {
        param.call().valueNotIn(values)
    }
}

/**
 * 小于，读 `{name}__lt`。
 */
inline fun <reified T : Any, reified P : Comparable<*>> FilterQueryScope<T>.`lt?`(
    param: KProperty<KExpression<P>>,
): KNonNullExpression<Boolean>? {
    val resolved = resolved(param)
    val value = call.queryParameter<P>(resolved.value, "lt")
    return param.call().`lt?`(value)
}

/**
 * 大于，读 `{name}__gt`。
 */
inline fun <reified T : Any, reified P : Comparable<*>> FilterQueryScope<T>.`gt?`(
    param: KProperty<KExpression<P>>,
): KNonNullExpression<Boolean>? {
    val resolved = resolved(param)
    val value = call.queryParameter<P>(resolved.value, "gt")
    return param.call().`gt?`(value)
}

/**
 * 小于等于，读 `{name}__le`。注意：与 [between?] 的 `__le` 参数同名，同一属性不要同时使用。
 */
inline fun <reified T : Any, reified P : Comparable<*>> FilterQueryScope<T>.`le?`(
    param: KProperty<KExpression<P>>,
): KNonNullExpression<Boolean>? {
    val resolved = resolved(param)
    val value = call.queryParameter<P>(resolved.value, "le")
    return param.call().`le?`(value)
}

/**
 * 大于等于，读 `{name}__ge`。注意：与 [between?] 的 `__ge` 参数同名，同一属性不要同时使用。
 */
inline fun <reified T : Any, reified P : Comparable<*>> FilterQueryScope<T>.`ge?`(
    param: KProperty<KExpression<P>>,
): KNonNullExpression<Boolean>? {
    val resolved = resolved(param)
    val value = call.queryParameter<P>(resolved.value, "ge")
    return param.call().`ge?`(value)
}

/**
 * 模糊匹配。参数 `{name}` 支持 ext 后缀：
 * `__anywhere`（默认）/ `__exact` / `__start` / `__end`。
 */
inline fun <reified T : Any> FilterQueryScope<T>.`ilike?`(
    param: KProperty<KExpression<String>>,
): KNonNullExpression<Boolean>? {
    val resolved = resolved(param)
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

/**
 * 区间匹配，读 `{name}__ge` / `{name}__le`。
 */
inline fun <reified T : Any, reified P : Comparable<*>> FilterQueryScope<T>.`between?`(
    param: KProperty<KExpression<P>>,
): KNonNullExpression<Boolean>? {
    val resolved = resolved(param)
    val parameter = call.queryParameterExt<P>(P::class, resolved.value)
    return param.call().`between?`(parameter["ge"]?.value, parameter["le"]?.value)
}

/**
 * 非空谓词（静态，不读参数）。
 */
inline fun <reified T : Any, reified P : Any> FilterQueryScope<T>.noNull(param: KProperty<KExpression<P>>)
    : KNonNullExpression<Boolean>? {
    return param.call().isNotNull()
}

/**
 * 为空谓词（静态，不读参数）。
 */
inline fun <reified T : Any, reified P : Any> FilterQueryScope<T>.isNull(param: KProperty<KExpression<P>>)
    : KNonNullExpression<Boolean>? {
    return param.call().isNull()
}

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
