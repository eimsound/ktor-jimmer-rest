package com.eimsound.ktor.route

import com.eimsound.jimmer.sqlClient
import com.eimsound.ktor.validator.exception.ValidationException
import com.eimsound.ktor.provider.*
import com.eimsound.util.jimmer.entityIdType
import com.eimsound.util.ktor.queryParameterValues
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import kotlin.reflect.KClass

/**
 * 保存操作（create/edit/patch）的公共配置契约。
 */
interface SaveProvider<T : Any> :
    InputProvider<T>, ValidatorProvider<T>, TransformProvider<T>, FetcherProvider<T> {

    var saveMode: SaveMode
    var associatedSaveMode: AssociatedSaveMode
}

@PublishedApi
internal suspend inline fun <reified TEntity : Any> RoutingCall.performSave(provider: SaveProvider<TEntity>) {
    val validator = provider.validator
    val input = provider.input
    val transformer = provider.transformer
    val result = when (input) {
        is Inputs.Entity -> {
            val body = receive<TEntity>()
            sqlClient.save(provider.prepareEntity(body), provider.saveMode, provider.associatedSaveMode)
        }
        is Inputs.InputType -> {
            val body = receive(input.inputType)
            validator.validate(body)
            val entity = transformer.transform(body)
            sqlClient.save(entity, provider.saveMode, provider.associatedSaveMode)
        }
    }
    respond(project(result.modifiedEntity, provider.fetcher))
}

/**
 * 校验并转换实体（Entity 入参路径）。
 */
@PublishedApi
internal fun <T : Any> SaveProvider<T>.prepareEntity(body: T): T {
    validator?.validate(body)
    return transformer.transform(body)
}

/**
 * 批量保存（createBatch/updateBatch 共用）：逐条校验 + 转换，`saveEntitiesCommand`。
 */
@PublishedApi
internal suspend inline fun <reified TEntity : Any> RoutingCall.performBatch(
    provider: SaveProvider<TEntity>,
) {
    val bodies = receive<List<TEntity>>()
    val entities = mutableListOf<TEntity>()
    val validationErrors = mutableListOf<String>()
    bodies.forEachIndexed { index, body ->
        try {
            entities.add(provider.prepareEntity(body))
        } catch (e: ValidationException) {
            validationErrors += e.errors.map { "第 ${index + 1} 条：$it" }
        }
    }
    if (validationErrors.isNotEmpty()) {
        throw ValidationException(HttpStatusCode.BadRequest, validationErrors)
    }
    val result = sqlClient.saveEntitiesCommand(entities) {
        setMode(provider.saveMode)
        setAssociatedModeAll(provider.associatedSaveMode)
    }.execute()
    respond(result.items.map { it.modifiedEntity })
}

/**
 * 批量删除（`?ids=...` 或重复参数），id 类型取自实体 `@Id` 属性。
 */
@PublishedApi
internal suspend inline fun <reified TEntity : Any> RoutingCall.performBatchDelete(
    idsParameterName: String,
) {
    @Suppress("UNCHECKED_CAST")
    val idType = entityIdType<TEntity>() as KClass<Any>
    val ids = queryParameterValues(idType, idsParameterName)
    sqlClient.deleteByIds(TEntity::class, ids)
    respond(HttpStatusCode.OK)
}

/**
 * 若配置了响应投影 fetcher，按 id 重新查询返回投影结果；否则返回原实体。
 */
@PublishedApi
internal fun <T : Any> project(entity: T, fetcher: Fetchers<T>?): Any {
    if (fetcher == null) {
        return entity
    }
    val spi = entity as? ImmutableSpi ?: return entity
    val id = spi.__type().idProp?.let { spi.__get(it.name) } ?: return entity
    return sqlClient.findById(fetcher, id) ?: entity
}
