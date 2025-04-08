package com.eimsound.ktor.route

import com.eimsound.jimmer.sqlClient
import com.eimsound.ktor.provider.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode

@KtorDsl
inline fun <reified TEntity : Any> Route.create(
    path: String = "",
    crossinline block: suspend CreateProvider<TEntity>.() -> Unit,
) = post(path) {
    val provider = CreateScope<TEntity>(call).apply { block() }
    val input = provider.input
    val result = when (input) {
        is Inputs.Entity -> {
            val entity = call.receive<TEntity>()
            provider.run { validator?.invoke(entity) }
            sqlClient.entities.save(entity, SaveMode.INSERT_ONLY, AssociatedSaveMode.MERGE)
        }
        is Inputs.InputEntity -> {
            val entity = call.receive(input.inputType)
            provider.run { validator?.invoke(entity) }
            sqlClient.entities.save(entity, SaveMode.INSERT_ONLY, AssociatedSaveMode.MERGE)
        }
    }
    call.respond(result.modifiedEntity)
}

interface CreateProvider<T : Any> : CallProvider, InputProvider<T>, ValidatorProvider<T>

class CreateScope<T : Any>(override val call: RoutingCall) : CreateProvider<T> {
    override var input: Inputs<T> = Inputs.Entity as Inputs<T>
    override var validator: Validators? = null
}

