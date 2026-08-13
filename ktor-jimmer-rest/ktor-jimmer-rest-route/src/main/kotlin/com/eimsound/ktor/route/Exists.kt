package com.eimsound.ktor.route

import com.eimsound.jimmer.sqlClient
import com.eimsound.ktor.config.Configuration
import com.eimsound.ktor.provider.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*

/**
 * 按 id 判断实体是否存在：`GET {path}/exists/{id}`（支持 key / keyResolver）。
 */
inline fun <reified TEntity : Any> Route.exists(
    path: String = Configuration.endpoint.existsPath,
    crossinline block: suspend QueryProvider<TEntity>.() -> Unit,
) = get(path) {
    val provider = QueryScope<TEntity>(call).apply { block() }
    val pathVariable = path.substringAfterLast('{').substringBefore('}')
    val key = call.resolveKey(provider, "{$pathVariable}")
    val exists = sqlClient.findById(TEntity::class, key) != null
    call.respond(exists)
}
