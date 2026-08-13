package com.eimsound.ktor.route

import com.eimsound.ktor.provider.*
import io.ktor.server.routing.*
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode

inline fun <reified TEntity : Any> Route.edit(
    path: String = "",
    crossinline block: suspend EditProvider<TEntity>.() -> Unit,
) = put(path) {
    val provider = EditScope<TEntity>(call).apply { block() }
    call.performSave(provider)
}

interface EditProvider<T : Any> : CallProvider, SaveProvider<T>

class EditScope<T : Any>(
    override val call: RoutingCall
) : EditProvider<T> {
    override var input: Inputs<T> = Inputs.Entity()
    override var validator: Validators<T>? = null
    override var transformer: Transformers<T>? = null
    override var fetcher: Fetchers<T>? = null
    override var saveMode: SaveMode = SaveMode.UPDATE_ONLY
    override var associatedSaveMode: AssociatedSaveMode = AssociatedSaveMode.UPDATE
}
