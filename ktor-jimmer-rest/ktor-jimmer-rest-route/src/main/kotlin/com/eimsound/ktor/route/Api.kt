package com.eimsound.ktor.route

import com.eimsound.ktor.config.Configuration
import io.ktor.server.routing.*

/**
 * 为一个 Jimmer 实体注册完整 CRUD 路由：`GET /{id}`、`GET`（列表）、`POST`、`PUT`，
 * 以及可选（`patch {}` 启用）的 `PATCH`、`DELETE /{id}`。
 *
 * block 只在注册时执行一次，构建 [ApiConfig]；依赖请求上下文的部分（filter、key）
 * 以请求期 lambda 形式存储。
 */
@JvmName("api")
inline fun <reified TEntity : Any> Route.api(
    path: String,
    pathVariable: String = Configuration.router.defaultPathVariable,
    crossinline block: ApiConfig<TEntity>.() -> Unit,
) {
    val config = ApiConfig<TEntity>().apply { block() }
    route(path) {
        id<TEntity>(pathVariable) {
            fetcher = config.fetcher
            key = config.key
            keyResolver = config.keyResolver
        }
        list<TEntity> {
            fetcher = config.fetcher
            filter = config.filter
            pager = config.pager
        }
        count<TEntity> {
            filter = config.filter
        }
        exists<TEntity> {
            key = config.key
            keyResolver = config.keyResolver
        }
        create<TEntity> {
            input = config.create.input
            validator = config.create.validator
            transformer = config.create.transformer
            saveMode = config.create.saveMode
            associatedSaveMode = config.create.associatedSaveMode
            fetcher = config.create.fetcher
        }
        edit<TEntity> {
            input = config.edit.input
            validator = config.edit.validator
            transformer = config.edit.transformer
            saveMode = config.edit.saveMode
            associatedSaveMode = config.edit.associatedSaveMode
            fetcher = config.edit.fetcher
        }
        if (config.patchEnabled) {
            patch<TEntity> {
                input = config.patch.input
                validator = config.patch.validator
                transformer = config.patch.transformer
                saveMode = config.patch.saveMode
                associatedSaveMode = config.patch.associatedSaveMode
                fetcher = config.patch.fetcher
            }
        }
        if (config.batchEnabled) {
            val batchConfig = config.batch
            if (batchConfig.createEnabled) {
                createBatch<TEntity>(batchConfig.path) {
                    input = config.create.input
                    validator = config.create.validator
                    transformer = config.create.transformer
                    saveMode = config.create.saveMode
                    associatedSaveMode = config.create.associatedSaveMode
                }
            }
            if (batchConfig.updateEnabled) {
                updateBatch<TEntity>(batchConfig.path) {
                    input = config.edit.input
                    validator = config.edit.validator
                    transformer = config.edit.transformer
                    saveMode = config.edit.saveMode
                    associatedSaveMode = config.edit.associatedSaveMode
                }
            }
            if (batchConfig.deleteEnabled) {
                deleteBatch<TEntity>(batchConfig.path, batchConfig.deleteIdsParameterName)
            }
        }
        remove<TEntity>(pathVariable) {
            key = config.key
            keyResolver = config.keyResolver
        }
    }
}
