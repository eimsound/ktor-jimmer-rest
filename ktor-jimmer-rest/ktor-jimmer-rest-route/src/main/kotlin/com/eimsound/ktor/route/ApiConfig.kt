package com.eimsound.ktor.route

import com.eimsound.ktor.provider.*
import com.eimsound.util.ktor.Pager
import io.ktor.server.routing.RoutingCall
import org.babyfish.jimmer.Input
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import kotlin.reflect.KClass

/**
 * `api<T>` 的注册期配置。`api` 的 block 只在路由注册时执行一次，
 * 依赖请求上下文的部分（filter、key）以请求期 lambda 形式存储。
 */
class ApiConfig<T : Any> :
    FilterProvider<T>, FetcherProvider<T>, KeyProvider<T>, PageProvider {

    override var fetcher: Fetchers<T>? = null
    override var filter: Filters<T>? = null
    override var key: Any? = null
    override var keyResolver: ((RoutingCall) -> Any?)? = null
    override var pager: Pager = Pager()

    val create = CreateConfig<T>()
    val edit = EditConfig<T>()

    /**
     * 是否注册 PATCH 路由（由 `patch {}` 块启用）。
     */
    var patchEnabled = false
}

/**
 * create 操作配置。
 */
class CreateConfig<T : Any> : SaveProvider<T> {
    override var input: Inputs<T> = Inputs.Entity()
    override var validator: Validators<T>? = null
    override var transformer: Transformers<T>? = null
    override var fetcher: Fetchers<T>? = null
    override var saveMode: SaveMode = SaveMode.INSERT_ONLY
    override var associatedSaveMode: AssociatedSaveMode = AssociatedSaveMode.MERGE
}

/**
 * edit/patch 操作配置。
 */
class EditConfig<T : Any> : SaveProvider<T> {
    override var input: Inputs<T> = Inputs.Entity()
    override var validator: Validators<T>? = null
    override var transformer: Transformers<T>? = null
    override var fetcher: Fetchers<T>? = null
    override var saveMode: SaveMode = SaveMode.UPDATE_ONLY
    override var associatedSaveMode: AssociatedSaveMode = AssociatedSaveMode.UPDATE
}

/**
 * 顶层 `input {}`：同时配置 create 与 edit（兼容原 api 语义）。
 */
fun <T : Any> ApiConfig<T>.input(block: EntityScope<T>.() -> Unit) {
    create.input(block)
    edit.input(block)
}

/**
 * 顶层 `input(InputType::class) {}`：同时配置 create 与 edit（兼容原 api 语义）。
 */
fun <T : Any, TInput : Input<T>> ApiConfig<T>.input(
    type: KClass<TInput>,
    block: InputScope<T, TInput>.() -> Unit,
) {
    create.input(type, block)
    edit.input(type, block)
}

/**
 * create 操作独立配置（保存模式、响应投影等）。
 */
fun <T : Any> ApiConfig<T>.create(block: CreateConfig<T>.() -> Unit) {
    this.create.apply(block)
}

/**
 * edit 操作独立配置（保存模式、响应投影等）。
 */
fun <T : Any> ApiConfig<T>.edit(block: EditConfig<T>.() -> Unit) {
    this.edit.apply(block)
}

/**
 * 启用 PATCH 路由并配置（复用 edit 配置；传入空块即可启用）。
 */
fun <T : Any> ApiConfig<T>.patch(block: EditConfig<T>.() -> Unit = {}) {
    patchEnabled = true
    this.edit.apply(block)
}
