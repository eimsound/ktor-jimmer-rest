package com.eimsound.ktor.jimmer.rest.route

import com.eimsound.ktor.jimmer.rest.validator.ValidationBuilder
import com.eimsound.ktor.jimmer.rest.provider.CallProvider
import com.eimsound.ktor.jimmer.rest.provider.EntityProvider
import com.eimsound.ktor.jimmer.rest.provider.ValidatorProvider
import com.eimsound.ktor.jimmer.rest.validator.validate
import com.eimsound.ktor.jimmer.rest.config.sqlClient
import com.eimsound.validator.ValidationResult
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*

@KtorDsl
inline fun <reified TEntity : Any> Route.create(
    path: String = "",
    crossinline block: suspend CreateProvider<TEntity>.(TEntity) -> Unit,
) = post(path) {
    val body = call.receive<TEntity>()
    val provider = CreateProvider.Impl<TEntity>(call).apply { block(body) }
    val entity = provider.entity ?: body

    provider.validator?.let {
        validate(entity, it)
    }.let {
        if (it is ValidationResult.Invalid) {
            call.response.status(HttpStatusCode.BadRequest)
            return@post call.respond(it.errors)
        }
    }

    val result = sqlClient.insert(entity)
    call.respond(result.modifiedEntity)
}

interface CreateProvider<T : Any> : CallProvider, EntityProvider<T>, ValidatorProvider<T> {
    class Impl<T : Any>(
        override val call: RoutingCall,
    ) : CreateProvider<T> {
        override var entity: T? = null
        override var validator: (ValidationBuilder.(T) -> Unit)? = null
    }
}

