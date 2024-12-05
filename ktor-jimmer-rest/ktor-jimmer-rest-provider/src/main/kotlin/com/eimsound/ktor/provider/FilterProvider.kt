package com.eimsound.ktor.provider

import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery

interface FilterProvider<T : Any> {
    var filter: (KMutableRootQuery<T>.() -> Unit)?
}

inline fun <T : Any> FilterProvider<T>.filter(noinline block: KMutableRootQuery<T>.() -> Unit) {
    filter = block
}
