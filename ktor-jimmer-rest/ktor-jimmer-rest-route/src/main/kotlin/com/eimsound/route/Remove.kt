package com.eimsound.ktor.jimmer.rest.route

import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import com.eimsound.ktor.jimmer.rest.provider.CallProvider
import com.eimsound.ktor.jimmer.rest.provider.KeyProvider
import com.eimsound.ktor.jimmer.rest.config.Configuration
import com.eimsound.ktor.jimmer.rest.jimmer.sqlClient
import com.eimsound.ktor.jimmer.rest.util.ktor.defaultPathVariable
import com.eimsound.ktor.jimmer.rest.util.parser.parse
import com.eimsound.ktor.jimmer.rest.util.reflect.jimmer.entityIdType

@KtorDsl
inline fun <reified TEntity : Any> Route.remove(
    path: String = Configuration.defaultPathVariable,
    crossinline block: suspend RemoveProvider<TEntity>.() -> Unit,
) = delete(path) {
    val provider = RemoveProvider.Impl<TEntity>(call).apply { block() }
    val key = provider.key ?: call.defaultPathVariable.parse(entityIdType<TEntity>())

    sqlClient.deleteById(TEntity::class, key)
    call.response.status(HttpStatusCode.OK)
}

interface RemoveProvider<T : Any> :
    CallProvider, KeyProvider<T> {
    class Impl<T : Any>(
        override val call: RoutingCall,
    ) : RemoveProvider<T> {
        override var key: Any? = null
    }
}


