package com.eimsound.ktor.provider

import com.eimsound.util.ktor.queryParameter
import com.eimsound.util.ktor.specification
import io.ktor.server.routing.*
import org.babyfish.jimmer.sql.kt.ast.query.KMutableQuery
import org.babyfish.jimmer.sql.kt.ast.query.specification.KSpecification
import kotlin.reflect.KClass


@DslMarker
annotation class FilterDslMarker

sealed class Filters<T : Any> {
    data class Filter<T : Any>(val filter: (query: KMutableQuery<T>, call: RoutingCall) -> Unit) : Filters<T>()
    data class Specification<T : Any>(val specification: (call: RoutingCall) -> KSpecification<T>) : Filters<T>()

    operator fun invoke(query: KMutableQuery<T>, call: RoutingCall) = when (this) {
        is Filter -> filter.invoke(query, call)
        is Specification -> specification
    }
}

@FilterDslMarker
interface FilterProvider<T : Any> {
    var filter: Filters<T>?
}

@FilterDslMarker
class FilterScope<T : Any>(private val query: KMutableQuery<T>, val call: RoutingCall) :
    KMutableQuery<T> by query {

    @FilterDslMarker
    operator inline fun <reified TParam : Any> get(key: String, ext: String? = null): TParam? =
        call.queryParameter<TParam>(key, ext)

}

fun <T : Any> FilterProvider<T>.filter(block: FilterScope<T>.() -> Unit) {
    filter = Filters.Filter { it, call ->
        block(FilterScope(it, call))
    }
}

inline fun <T : Any> FilterProvider<T>.filter(specificationType: KClass<out KSpecification<T>>) {
    filter = Filters.Specification {
        specification(specificationType, it)
    }
}

