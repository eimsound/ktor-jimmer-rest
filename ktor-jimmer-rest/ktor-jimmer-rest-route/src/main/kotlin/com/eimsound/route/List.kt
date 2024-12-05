package com.eimsound.ktor.jimmer.rest.route

import com.eimsound.ktor.jimmer.rest.provider.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.query.KMutableRootQuery
import com.eimsound.ktor.jimmer.rest.jimmer.sqlClient
import com.eimsound.ktor.jimmer.rest.util.ktor.queryParameter

@KtorDsl
inline fun <reified TEntity : Any> Route.list(
    crossinline block: suspend ListProvider<TEntity>.() -> Unit,
) = get {
    val provider = ListProvider.Impl<TEntity>(call).apply { block() }

    val pager = provider.pager.apply {
        pageIndex = pageIndex ?: call.queryParameter<Int>(pageIndexParameterName) ?: pageIndex
        pageSize = pageSize ?: call.queryParameter<Int>(pageSizeParameterName) ?: pageSize
    }
    val filter = provider.filter
    val fetcher = provider.fetcher

    val result = sqlClient.createQuery(TEntity::class) {
        filter?.invoke(this)
        select(table.fetch(fetcher))
    }.fetchPageOrElse(pager) {
        execute()
    }
    call.respond(result)
}

interface ListProvider<T : Any> :
    FetcherProvider<T>,
    FilterProvider<T>,
    PageProvider,
    CallProvider,
    ConditionProvider<T> {
    class Impl<T : Any>(
        override val call: RoutingCall,
    ) : ListProvider<T> {
        override var fetcher: Fetcher<T>? = null
        override var filter: (KMutableRootQuery<T>.() -> Unit)? = null
        override var pager: Pager = Pager()
    }
}
