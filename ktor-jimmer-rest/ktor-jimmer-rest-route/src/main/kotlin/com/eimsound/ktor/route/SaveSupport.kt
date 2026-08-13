package com.eimsound.ktor.route

import com.eimsound.jimmer.sqlClient
import com.eimsound.ktor.provider.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode

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
            validator.validate(body)
            val entity = transformer.transform(body)
            sqlClient.save(entity, provider.saveMode, provider.associatedSaveMode)
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
