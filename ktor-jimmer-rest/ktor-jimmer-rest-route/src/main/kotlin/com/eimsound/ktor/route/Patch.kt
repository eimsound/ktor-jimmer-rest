package com.eimsound.ktor.route

import com.eimsound.ktor.provider.*
import io.ktor.server.routing.*
import io.ktor.server.routing.patch as ktorPatch

/**
 * PATCH 部分更新路由：与 PUT 相同的 `UPDATE_ONLY` 保存语义。
 * 请求体中缺失的字段不会被覆盖（Jimmer 未加载属性不更新）。
 */
inline fun <reified TEntity : Any> Route.patch(
    path: String = "",
    crossinline block: suspend EditProvider<TEntity>.() -> Unit,
) = ktorPatch(path) {
    val provider = EditScope<TEntity>(call).apply { block() }
    call.performSave(provider)
}
