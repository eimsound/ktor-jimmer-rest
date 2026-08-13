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
import com.eimsound.util.ktor.pathParameter
import com.eimsound.util.parser.parse
import com.eimsound.util.jimmer.entityIdType

inline fun <reified TEntity : Any> Route.id(
    pathVariable: String = Configuration.router.defaultPathVariable,
    crossinline block: suspend QueryProvider<TEntity>.() -> Unit,
) = get(pathVariable) {
    val provider = QueryScope<TEntity>(call).apply { block() }
    val key = call.resolveKey(provider, pathVariable)
    val fetcher = provider.fetcher
    val result = if (fetcher != null) {
        sqlClient.findById(fetcher, key)
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
    override var keyResolver: ((RoutingCall) -> Any?)? = null
}

/**
 * 解析 id/remove 的 key：静态 key → keyResolver → 路径参数。
 */
@PublishedApi
internal inline fun <reified TEntity : Any> RoutingCall.resolveKey(
    provider: KeyProvider<TEntity>,
    pathVariable: String,
): Any = provider.key
    ?: provider.keyResolver?.invoke(this)
    ?: pathParameter(pathVariable.removeSurrounding("{", "}")).parse(entityIdType<TEntity>())
