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
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import org.babyfish.jimmer.sql.kt.ast.query.KMutableQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.specification.KSpecification
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullProps
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
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

    override fun resolved(expression: KPropExpression<*>): ResolvedName {
        val resolved = ParameterNames.resolveExpression(expression)
        ParameterNames.ensureNoRootCollision(table, resolved)
        return resolved
    }

    /**
     * 关联过滤入口：`where(table.store) { ... }`（引用关联，表对象）或
     * `where<Author>(table.authors) { ... }`（集合关联）。
     * 底层使用 Jimmer 隐式子查询（EXISTS 语义），与 `table.authors {}` 一致，
     * 避免显式 JOIN 带来的数据重复与分页失效。
     *
     * @param TRelated 关联实体类型。
     * @param prop 关联表对象（如 `table.store`）或集合关联入口。
     * @param block 子表过滤块，receiver 为 [AssociationFilterScope]，可使用全部 filter 操作符。
     */
    fun <TRelated : Any> where(
        prop: KNonNullTable<TRelated>,
        block: AssociationFilterScope<TRelated>.() -> KNonNullExpression<Boolean>?,
    ) {
        val requestCall = call
        val propName = ParameterNames.associationNameOf(prop)
        where(table.exists<TRelated>(propName) {
            block(AssociationFilterScope(this, requestCall, propName))
        })
    }

    /**
     * 集合关联过滤入口：`where(Book::authors) { ... }`。
     * 底层使用 Jimmer 隐式子查询（EXISTS 语义）。
     */
    fun <TRelated : Any> where(
        prop: KProperty1<T, List<TRelated>>,
        block: AssociationFilterScope<TRelated>.() -> KNonNullExpression<Boolean>?,
    ) {
        val requestCall = call
        where(table.exists<TRelated>(prop.name) {
            block(AssociationFilterScope(this, requestCall, prop.name))
        })
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

    /**
     * 解析属性表达式 → 查询参数名，并做根表冲突检查。
     */
    fun resolved(expression: KPropExpression<*>): ResolvedName
}

/**
 * 关联子表过滤作用域：`where(Book::authors) { ... }` 块内的 receiver。
 * 持有子表（table）+ 请求上下文（call）+ 关联参数名前缀（如 `authors`）。
 * 块内通过子表属性引用（`table::firstName`）配合 `ilike?` 等操作符使用，
 * 参数名由前缀 + 属性名自动解析（如 `authors_firstName`），不依赖运行时 receiver 绑定。
 */
@FilterDslMarker
class AssociationFilterScope<T : Any>(
    innerTable: KNonNullTable<T>,
    override val call: RoutingCall,
    @PublishedApi
    internal val prefix: String,
) : KNonNullProps<T> by innerTable, FilterQueryScope<T> {
    override val table: KNonNullTable<T> = innerTable

    /**
     * 嵌套关联过滤通用入口：`assoc<Book>("books") { ... }`。
     * 内部通过 Jimmer `KProps.exists` 生成 EXISTS 谓词。
     */
    inline fun <TRelated : Any> assoc(
        propName: String,
        crossinline block: AssociationFilterScope<TRelated>.() -> KNonNullExpression<Boolean>?,
    ): KNonNullExpression<Boolean>? {
        val nestedPrefix = prefix + Configuration.router.subParameterSeparator + propName
        val requestCall = call
        return table.exists<TRelated>(propName) {
            block(AssociationFilterScope(this, requestCall, nestedPrefix))
        }
    }

    /**
     * 嵌套关联过滤的编译期安全形式：`assoc(BookStore::books) { ... }`。
     * 从实体属性引用提取关联名，写错属性名编译期即失败。
     */
    inline fun <TRelated : Any> assoc(
        prop: KProperty1<T, List<TRelated>>,
        crossinline block: AssociationFilterScope<TRelated>.() -> KNonNullExpression<Boolean>?,
    ): KNonNullExpression<Boolean>? = assoc<TRelated>(prop.name, block)

    /**
     * 嵌套关联过滤的表方法引用形式：`assoc(table::books) { ... }`。
     * `books` 是 KSP 在表上生成的扩展方法，方法引用自带关联名。
     */
    inline fun <TRelated : Any> assoc(
        prop: KFunction<*>,
        crossinline block: AssociationFilterScope<TRelated>.() -> KNonNullExpression<Boolean>?,
    ): KNonNullExpression<Boolean>? = assoc<TRelated>(prop.name, block)

    override fun resolved(property: KProperty<KExpression<*>>): ResolvedName {
        val resolved = ResolvedName(
            value = prefix + Configuration.router.subParameterSeparator + property.name,
            segments = listOf(prefix),
            propertyName = property.name,
        )
        ParameterNames.ensureNoRootCollision(table, resolved)
        return resolved
    }

    override fun resolved(expression: KPropExpression<*>): ResolvedName {
        val relative = ParameterNames.resolveExpression(expression)
        val value = if (prefix.isEmpty()) {
            relative.value
        } else {
            prefix + Configuration.router.subParameterSeparator + relative.value
        }
        val resolved = ResolvedName(
            value = value,
            segments = listOf(prefix) + relative.segments,
            propertyName = relative.propertyName,
        )
        ParameterNames.ensureNoRootCollision(table, resolved)
        return resolved
    }
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
