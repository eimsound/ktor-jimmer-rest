package com.eimsound.ktor.provider

import com.eimsound.util.ktor.queryParameter
import io.ktor.server.routing.*
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery

@FilterDslMarker
interface FilterProvider<T : Any> {
    var filter: (FilterScope<T>.() -> Unit)?
}

@FilterDslMarker
class FilterScope<T : Any>(query: KMutableRootQuery<T>, val call: RoutingCall) :
    KMutableRootQuery<T> by query {

    inline fun <reified TParam : Any> param(key: String, ext: String? = null): TParam? =
        call.queryParameter<TParam>(key, ext)

}

fun <T : Any> FilterProvider<T>.filter(block: FilterScope<T>.() -> Unit) {
    filter = block
}

@DslMarker
annotation class FilterDslMarker
