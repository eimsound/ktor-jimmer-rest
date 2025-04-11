package com.eimsound.ktor.route

import com.eimsound.ktor.provider.CallProvider
import com.eimsound.ktor.provider.InputProvider
import com.eimsound.ktor.provider.ValidatorProvider
import com.eimsound.jimmer.sqlClient
import com.eimsound.ktor.provider.Inputs
import com.eimsound.ktor.provider.TransformProvider
import com.eimsound.ktor.provider.Transformers
import com.eimsound.ktor.provider.Validators
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode

@KtorDsl
inline fun <reified TEntity : Any> Route.edit(
    path: String = "",
    crossinline block: suspend EditProvider<TEntity>.() -> Unit,
) = put(path) {
    val provider = EditScope<TEntity>(call).apply { block() }
    val input = provider.input
    val validator = provider.validator
    val result = when (input) {
        is Inputs.Entity -> {
            val body = call.receive<TEntity>()
            validator?.invoke(body)
            val entity = provider.transformer?.transform(body) ?: body
            sqlClient.entities.save(entity, SaveMode.UPDATE_ONLY, AssociatedSaveMode.UPDATE)
        }

        is Inputs.InputEntity -> {
            val body = call.receive(input.inputType)
            validator?.invoke(body)
            val entity = provider.transformer?.transform(body) ?: body
            sqlClient.entities.save(entity, SaveMode.UPDATE_ONLY, AssociatedSaveMode.UPDATE)
        }
    }
    call.respond(result.modifiedEntity)
}

interface EditProvider<T : Any> : CallProvider, InputProvider<T>, ValidatorProvider, TransformProvider<T>

class EditScope<T : Any>(override val call: RoutingCall) : EditProvider<T> {
    override var input: Inputs<T> = Inputs.Entity as Inputs<T>
    override var validator: Validators? = null
    override var transformer: Transformers<T>? = null
}
