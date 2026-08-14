package com.eimsound.ktor.provider

import com.eimsound.ktor.provider.Filters.Filter
import com.eimsound.ktor.provider.Filters.Specification
import com.eimsound.ktor.config.Configuration
import com.eimsound.util.jimmer.KSpecificationQuery
import com.eimsound.util.ktor.specification
import com.eimsound.util.ktor.ParameterNames
import com.eimsound.util.ktor.ResolvedName
import io.ktor.server.routing.RoutingCall
import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.query.KMutableQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.specification.KSpecification
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1


@DslMarker
annotation class FilterDslMarker

sealed class Filters<T> {
    data class Filter<T : Any>(val filter: (query: KMutableRootQuery.ForEntity<T>, call: RoutingCall) -> Unit) :
        Filters<T>()

    data class Specification<T : Any>(val specification: (query: KMutableRootQuery.ForEntity<T>, call: RoutingCall) -> KSpecification<T>) :
        Filters<T>()
}

operator fun <T : Any> Filters<T>?.invoke(query: KMutableRootQuery.ForEntity<T>, call: RoutingCall) = this?.run {
    when (this) {
        is Filter -> filter(query, call)
        is Specification -> query.where(specification(query, call))
    }
}

@FilterDslMarker
interface FilterProvider<T : Any> {
    var filter: Filters<T>?
}

@FilterDslMarker
class FilterScope<T : Any>(query: KMutableQuery<KNonNullTable<T>>, override val call: RoutingCall) :
    KMutableQuery<KNonNullTable<T>> by query, FilterQueryScope<T> {
    override val table: KNonNullTable<T> = query.table

    override fun resolved(property: KProperty<KExpression<*>>): ResolvedName {
        val resolved = ParameterNames.resolveWithPath(property)
        ParameterNames.ensureNoRootCollision(table, resolved)
        return resolved
    }
}

/**
 * filter 操作符（eq?/ilike?/between? 等）的最小依赖契约。
 * 根表作用域 [FilterScope] 与关联子表作用域 [AssociationFilterScope] 都实现它，
 * 使操作符在两种上下文下可复用。
 */
@FilterDslMarker
interface FilterQueryScope<T : Any> {
    val table: KNonNullTable<T>
    val call: RoutingCall

    /**
     * 解析属性引用 → 查询参数名，并做根表冲突检查。
     */
    fun resolved(property: KProperty<KExpression<*>>): ResolvedName
}

/**
 * 关联子表过滤作用域：`where(Book::authors) { ... }` 块内的 receiver。
 * 持有子表（table）+ 请求上下文（call）+ 关联参数名前缀（如 `authors`）。
 * 块内通过子表属性引用（`table::firstName`）配合 `ilike?` 等操作符使用，
 * 参数名由前缀 + 属性名自动解析（如 `authors_firstName`），不依赖运行时 receiver 绑定。
 */
@FilterDslMarker
class AssociationFilterScope<T : Any>(
    override val table: KNonNullTable<T>,
    override val call: RoutingCall,
    @PublishedApi
    internal val prefix: String,
) : FilterQueryScope<T> {
    override fun resolved(property: KProperty<KExpression<*>>): ResolvedName {
        val resolved = ResolvedName(
            value = prefix + Configuration.router.subParameterSeparator + property.name,
            segments = listOf(prefix),
            propertyName = property.name,
        )
        ParameterNames.ensureNoRootCollision(table, resolved)
        return resolved
    }
}

/**
 * 关联块内的嵌套关联过滤：`where(Book::authors) { where(Author::books) { ... } }`。
 * 返回 EXISTS 谓词（由外层块作为最后一个表达式返回），参数名前缀累积：
 * `authors` → `authors_books`。
 */
inline fun <T : Any, TRelated : Any> AssociationFilterScope<T>.where(
    prop: KProperty1<T, List<TRelated>>,
    crossinline block: AssociationFilterScope<TRelated>.() -> KNonNullExpression<Boolean>?,
): KNonNullExpression<Boolean>? {
    return table.exists<TRelated>(prop.name) {
        val nestedPrefix = prefix + Configuration.router.subParameterSeparator + prop.name
        block(AssociationFilterScope(this, this@where.call, nestedPrefix))
    }
}

/**
 * 关联过滤入口：`where(Book::authors) { `ilike?`(table::firstName) }`。
 * 底层使用 Jimmer 隐式子查询（EXISTS 语义），与 `table.authors {}` 一致，
 * 避免显式 JOIN 带来的数据重复与分页失效。
 *
 * @param prop 关联属性引用（如 `Book::authors`），编译期类型安全。
 * @param block 子表过滤块，receiver 为 [AssociationFilterScope]，可使用全部 filter 操作符。
 */
inline fun <T : Any, TRelated : Any> FilterScope<T>.where(
    prop: KProperty1<T, List<TRelated>>,
    crossinline block: AssociationFilterScope<TRelated>.() -> KNonNullExpression<Boolean>?,
): Unit {
    where(table.exists<TRelated>(prop.name) {
        block(AssociationFilterScope(this, this@where.call, prop.name))
    })
}

/**
 * SpecificationScope
 * [org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery] 权限太大
 * 包括[org.babyfish.jimmer.sql.kt.ast.query.KRootSelectable] 里的api
 * 为了防止使用到，遂代理此类 [org.babyfish.jimmer.sql.kt.ast.query.KMutableQuery]
 */
@FilterDslMarker
class SpecificationScope<T : Any>(private val query: KMutableQuery<KNonNullTable<T>>, val call: RoutingCall) :
    KSpecificationQuery<T> {
    override val table: KNonNullTable<T> = query.table
    override fun orderBy(vararg orders: Order?) = query.orderBy(*orders)

    override fun orderBy(vararg expressions: KExpression<T>?) = query.orderBy(*expressions)

    override fun orderBy(orders: List<Order?>) = query.orderBy(orders)

    override fun groupBy(vararg expressions: KExpression<T>) = query.groupBy(*expressions)

    override fun having(vararg predicates: KNonNullExpression<Boolean>?) = query.having(*predicates)
}

inline fun <T : Any> FilterProvider<T>.filter(crossinline block: FilterScope<T>.() -> Unit) {
    filter = Filter { it, call ->
        block(FilterScope(it, call))
    }
}

inline fun <T : Any> FilterProvider<T>.filter(
    specificationType: KClass<out KSpecification<T>>, crossinline block: SpecificationScope<T>.() -> Unit
) {
    filter = Specification { it, call ->
        block(SpecificationScope(it, call))
        call.specification(specificationType)
    }
}

fun <T : Any> FilterProvider<T>.filter(
    specificationType: KClass<out KSpecification<T>>,
) {
    filter = Specification { it, call ->
        call.specification(specificationType)
    }
}
