package com.eimsound.ktor.route

import com.eimsound.ktor.provider.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.eimsound.jimmer.sqlClient
import com.eimsound.util.ktor.queryParameter
import com.eimsound.util.ktor.Pager
import com.eimsound.util.jimmer.fetchPageOrElse
import io.ktor.utils.io.*

@KtorDsl
inline fun <reified TEntity : Any> Route.list(
    crossinline block: suspend ListProvider<TEntity>.() -> Unit,
) = get {
    val provider = ListScope<TEntity>(call).apply { block() }

    val pager = provider.pager.apply {
        pageIndex = call.queryParameter<Int>(pageIndexParameterName) ?: pageIndex
        pageSize = call.queryParameter<Int>(pageSizeParameterName) ?: pageSize
    }
    val filter = provider.filter
    val fetcher = provider.fetcher

    val result = sqlClient.createQuery(TEntity::class) {
        filter.invoke(this, call)
        select(fetcher.invoke(table) ?: table)
    }.fetchPageOrElse(pager) {
        execute()
    }

    call.respond(result)
}


interface ListProvider<T : Any> : FetcherProvider<T>, FilterProvider<T>, PageProvider, CallProvider

class ListScope<T : Any>(
    override val call: RoutingCall
) : ListProvider<T> {
    override var fetcher: Fetchers<T>? = null
    override var filter: Filters<T>? = null
    override var pager: Pager = Pager()
}
