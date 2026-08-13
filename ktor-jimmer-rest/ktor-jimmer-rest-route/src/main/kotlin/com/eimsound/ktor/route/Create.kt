package com.eimsound.ktor.route

import com.eimsound.ktor.provider.*
import io.ktor.server.routing.*
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode

inline fun <reified TEntity : Any> Route.create(
    path: String = "",
    crossinline block: suspend CreateProvider<TEntity>.() -> Unit,
) = post(path) {
    val provider = CreateScope<TEntity>(call).apply { block() }
    call.performSave(provider)
}

interface CreateProvider<T : Any> : CallProvider, SaveProvider<T>

class CreateScope<T : Any>(
    override val call: RoutingCall
) : CreateProvider<T> {
    override var input: Inputs<T> = Inputs.Entity()
    override var validator: Validators<T>? = null
    override var transformer: Transformers<T>? = null
    override var fetcher: Fetchers<T>? = null
    override var saveMode: SaveMode = SaveMode.INSERT_ONLY
    override var associatedSaveMode: AssociatedSaveMode = AssociatedSaveMode.MERGE
}
