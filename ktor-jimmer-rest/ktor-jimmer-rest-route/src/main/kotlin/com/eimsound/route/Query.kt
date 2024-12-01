package com.eimsound.ktor.jimmer.rest.route

import com.eimsound.ktor.jimmer.rest.provider.CallProvider
import com.eimsound.ktor.jimmer.rest.provider.FetcherProvider
import com.eimsound.ktor.jimmer.rest.provider.KeyProvider
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import org.babyfish.jimmer.sql.fetcher.Fetcher
import com.eimsound.ktor.jimmer.rest.config.Configuration
import com.eimsound.ktor.jimmer.rest.config.sqlClient
import com.eimsound.ktor.jimmer.rest.provider.defaultPathVariable
import com.eimsound.ktor.jimmer.rest.provider.entityIdType
import com.eimsound.ktor.jimmer.rest.util.reflect.parse

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
