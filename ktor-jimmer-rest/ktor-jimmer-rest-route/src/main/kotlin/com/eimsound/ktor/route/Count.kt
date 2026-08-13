package com.eimsound.ktor.route

import com.eimsound.jimmer.sqlClient
import com.eimsound.ktor.config.Configuration
import com.eimsound.ktor.provider.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.babyfish.jimmer.sql.kt.ast.expression.rowCount

/**
 * 过滤后的计数端点（忽略分页与排序）：`GET {path}/count`。
 */
inline fun <reified TEntity : Any> Route.count(
    path: String = Configuration.endpoint.countPath,
    crossinline block: suspend ListProvider<TEntity>.() -> Unit,
) = get(path) {
    val provider = ListScope<TEntity>(call).apply { block() }
    val filter = provider.filter
    val count = sqlClient.createQuery(TEntity::class) {
        filter.invoke(this, call)
        select(rowCount())
    }.fetchUnlimitedCount()
    call.respond(count)
}
