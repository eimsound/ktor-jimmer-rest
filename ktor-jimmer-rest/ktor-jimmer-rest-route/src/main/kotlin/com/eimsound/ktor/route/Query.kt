package com.eimsound.ktor.route

import com.eimsound.ktor.provider.CallProvider
import com.eimsound.ktor.provider.FetcherProvider
import com.eimsound.ktor.provider.KeyProvider
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.eimsound.ktor.config.Configuration
import com.eimsound.jimmer.sqlClient
import com.eimsound.ktor.provider.Fetchers
import com.eimsound.util.ktor.defaultPathVariable
import com.eimsound.util.parser.parse
import com.eimsound.util.jimmer.entityIdType
import io.ktor.utils.io.*

@KtorDsl
inline fun <reified TEntity : Any> Route.id(
    pathVariable: String = Configuration.router.defaultPathVariable,
    crossinline block: suspend QueryProvider<TEntity>.() -> Unit,
) = get(pathVariable) {
    val provider = QueryScope<TEntity>(call).apply { block() }
    val key = provider.key ?: call.defaultPathVariable.parse(entityIdType<TEntity>())
    val fetcher = provider.fetcher
    val result = if (fetcher != null) {
        when (fetcher) {
            is Fetchers.Fetch<TEntity> ->
                sqlClient.findById(fetcher.fetcher, key)
            is Fetchers.ViewType<TEntity> ->
                sqlClient.findById(fetcher.viewType, key)
        }
    } else {
        sqlClient.findById(TEntity::class, key)
    }

    if (result != null) {
        call.respond(result)
    } else {
        call.response.status(HttpStatusCode.NotFound)
    }
}

interface QueryProvider<T : Any> : FetcherProvider<T>, CallProvider, KeyProvider<T>

class QueryScope<T : Any>(override val call: RoutingCall) : QueryProvider<T> {
    override var fetcher: Fetchers<T>? = null
    override var key: Any? = null
}

