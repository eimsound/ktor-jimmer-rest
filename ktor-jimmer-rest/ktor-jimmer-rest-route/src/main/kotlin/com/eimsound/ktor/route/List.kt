package com.eimsound.ktor.route

import com.eimsound.ktor.provider.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import com.eimsound.jimmer.sqlClient
import com.eimsound.ktor.config.Configuration
import com.eimsound.ktor.provider.Fetchers.Fetch
import com.eimsound.ktor.provider.Fetchers.ViewType
import com.eimsound.util.ktor.queryParameter
import com.eimsound.util.ktor.Pager
import com.eimsound.util.jimmer.fetchPageOrElse
import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import kotlin.reflect.KClass

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
        filter?.invoke(FilterScope(this, call))
        select(fetcher?.fetch(table) ?: table)
    }.fetchPageOrElse(pager) {
        execute()
    }

    call.respond(result)
}

fun <T : Any> Fetchers<T>.fetch(table: KNonNullTable<T>) = when (this) {
    is ViewType -> table.fetch(viewType)
    is Fetch -> table.fetch(fetcher)
}

interface ListProvider<T : Any> : FetcherProvider<T>, FilterProvider<T>, PageProvider, CallProvider

class ListScope<T : Any>(
    override val call: RoutingCall
) : ListProvider<T> {
    override var fetcher: Fetchers<T>? = null
    override var filter: (FilterScope<T>.() -> Unit)? = null
    override var pager: Pager = Pager()
}
