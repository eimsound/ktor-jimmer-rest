package com.eimsound.ktor.provider

import com.eimsound.ktor.provider.Filters.Filter
import com.eimsound.ktor.provider.Filters.Specification
import com.eimsound.util.jimmer.KSpecificationQuery
import com.eimsound.util.ktor.queryParameter
import com.eimsound.util.ktor.specification
import io.ktor.server.routing.*
import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.query.KMutableQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import org.babyfish.jimmer.sql.kt.ast.query.specification.KSpecification
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import kotlin.reflect.KClass


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
class FilterScope<T : Any>(query: KMutableQuery<KNonNullTable<T>>, val call: RoutingCall) :
    KMutableQuery<KNonNullTable<T>> by query {

    inline operator fun <reified TParam : Any> get(key: String, ext: String? = null): TParam? =
        call.queryParameter<TParam>(key, ext)

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

    inline operator fun <reified TParam : Any> get(key: String, ext: String? = null): TParam? =
        call.queryParameter<TParam>(key, ext)

    override fun orderBy(vararg orders: Order?) = query.orderBy(*orders)

    override fun orderBy(vararg expressions: KExpression<T>?) = query.orderBy(*expressions)

    override fun orderBy(orders: List<Order?>) = query.orderBy(orders)

    override fun groupBy(vararg expressions: KExpression<T>) = query.groupBy(*expressions)

    override fun having(vararg predicates: KNonNullExpression<Boolean>?) = query.having(*predicates)
}

inline fun <T : Any> FilterProvider<T>.filter(crossinline block:  FilterScope<T>.() -> Unit) {
    filter = Filters.Filter { it, call ->
        block(FilterScope(it, call))
    }
}

inline fun <T : Any> FilterProvider<T>.filter(
    specificationType: KClass<out KSpecification<T>>,
    crossinline block: SpecificationScope<T>.() -> Unit
) {
    filter = Filters.Specification { it, call ->
        block(SpecificationScope(it, call))
        specification(specificationType, call)
    }
}

fun <T : Any> FilterProvider<T>.filter(
    specificationType: KClass<out KSpecification<T>>,
) {
    filter = Filters.Specification { it, call ->
        specification(specificationType, call)
    }
}
