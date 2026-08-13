package com.eimsound.ktor.route

import com.eimsound.ktor.provider.*
import com.eimsound.ktor.config.Configuration
import io.ktor.server.routing.*

/**
 * 批量创建：JSON 数组，逐条校验 + 转换，使用 create 配置的 SaveMode（`saveEntitiesCommand`）。
 */
inline fun <reified TEntity : Any> Route.createBatch(
    path: String = Configuration.endpoint.batchPath,
    crossinline block: suspend CreateProvider<TEntity>.() -> Unit,
) = post(path) {
    call.performBatch(CreateScope<TEntity>(call).apply { block() })
}

/**
 * 批量更新：JSON 数组，使用 edit 配置的 SaveMode（`saveEntitiesCommand`）。
 */
inline fun <reified TEntity : Any> Route.updateBatch(
    path: String = Configuration.endpoint.batchPath,
    crossinline block: suspend EditProvider<TEntity>.() -> Unit,
) = put(path) {
    call.performBatch(EditScope<TEntity>(call).apply { block() })
}

/**
 * 批量删除：`?ids=1,2,3`（或重复参数），id 类型取自实体 `@Id` 属性。
 */
inline fun <reified TEntity : Any> Route.deleteBatch(
    path: String = Configuration.endpoint.batchPath,
    idsParameterName: String = Configuration.endpoint.batchIdsParameterName,
) = delete(path) {
    call.performBatchDelete<TEntity>(idsParameterName)
}
