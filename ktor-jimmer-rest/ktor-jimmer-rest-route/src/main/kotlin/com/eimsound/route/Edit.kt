package com.eimsound.ktor.jimmer.rest.route

import com.eimsound.ktor.jimmer.rest.validator.ValidationBuilder
import com.eimsound.ktor.jimmer.rest.provider.CallProvider
import com.eimsound.ktor.jimmer.rest.provider.EntityProvider
import com.eimsound.ktor.jimmer.rest.provider.ValidatorProvider
import com.eimsound.ktor.jimmer.rest.jimmer.sqlClient
import com.eimsound.ktor.jimmer.rest.provider.validate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*


@KtorDsl
inline fun <reified TEntity : Any> Route.edit(
    path: String = "",
    crossinline block: suspend EditProvider<TEntity>.(TEntity) -> Unit,
) = patch(path) {
    val body = call.receive<TEntity>()
    val provider = EditProvider.Impl<TEntity>(call).apply { block(body) }.apply{
        validator?.validate(body)
    }

    val entity = provider.entity?.invoke(body) ?: body

    val result = sqlClient.update(entity)
    call.respond(result.modifiedEntity)
}

interface EditProvider<T : Any> : CallProvider, EntityProvider<T>,
    ValidatorProvider<T> {

    class Impl<T : Any>(
        override val call: RoutingCall,
    ) : EditProvider<T> {
        override var entity: ((T) -> T)? = null
        override var validator: (ValidationBuilder.(T) -> Unit)? = null
    }
}
