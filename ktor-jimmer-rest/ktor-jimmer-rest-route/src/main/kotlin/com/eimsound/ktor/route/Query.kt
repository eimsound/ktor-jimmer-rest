package com.eimsound.ktor.route

import com.eimsound.ktor.provider.CallProvider
import com.eimsound.ktor.provider.FetcherProvider
import com.eimsound.ktor.provider.KeyProvider
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import org.babyfish.jimmer.sql.fetcher.Fetcher
import com.eimsound.ktor.config.Configuration
import com.eimsound.jimmer.sqlClient
import com.eimsound.util.ktor.defaultPathVariable
import com.eimsound.util.parser.parse
import com.eimsound.util.jimmer.entityIdType

@KtorDsl
inline fun <reified TEntity : Any> Route.id(
    path: String = Configuration.defaultPathVariable,
    crossinline block: suspend QueryProvider<TEntity>.() -> Unit,
) = get(path) {

    val provider = QueryProvider.Impl<TEntity>(call).apply { block() }
    val fetcher = provider.fetcher

    val key = provider.key ?: call.defaultPathVariable.parse(entityIdType<TEntity>())

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

interface QueryProvider<T : Any> : FetcherProvider<T>, CallProvider, KeyProvider<T> {

    class Impl<T : Any>(
        override val call: RoutingCall,
    ) : QueryProvider<T> {
        override var fetcher: Fetcher<T>? = null
        override var key: Any? = null
    }
}
