package com.eimsound.ktor.route

import com.eimsound.ktor.validator.ValidationBuilder
import com.eimsound.ktor.provider.CallProvider
import com.eimsound.ktor.provider.EntityProvider
import com.eimsound.ktor.provider.ValidatorProvider
import com.eimsound.jimmer.sqlClient
import com.eimsound.ktor.provider.validate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import org.babyfish.jimmer.Input
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode


@KtorDsl
inline fun <reified TEntity : Any> Route.edit(
    path: String = "",
    crossinline block: suspend EditProvider<TEntity>.() -> Unit,
) = put(path) {
    val body = call.receive<TEntity>()
    val provider = EditScope<TEntity>(call).apply { block() }.apply {
        validator?.validate(body)
    }
    val entity = provider.entity?.invoke(body) ?: body
    val result = if (entity is Input<*>) {
        sqlClient.entities.save(entity, SaveMode.UPDATE_ONLY, AssociatedSaveMode.UPDATE)
    } else {
        sqlClient.entities.save(entity, SaveMode.UPDATE_ONLY, AssociatedSaveMode.UPDATE)
    }
    call.respond(result.modifiedEntity)
}

interface EditProvider<T : Any> : CallProvider, EntityProvider<T>, ValidatorProvider<T>

class EditScope<T : Any>(override val call: RoutingCall) : EditProvider<T> {
    override var entity: ((T) -> T)? = null
    override var validator: (ValidationBuilder.(T) -> Unit)? = null
}
