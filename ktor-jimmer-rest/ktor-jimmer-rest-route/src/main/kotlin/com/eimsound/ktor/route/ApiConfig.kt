package com.eimsound.ktor.route

import com.eimsound.ktor.provider.*
import com.eimsound.ktor.config.Configuration
import com.eimsound.util.ktor.Pager
import io.ktor.server.routing.Route
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
    val patch = EditConfig<T>()

    /**
     * 是否注册 PATCH 路由（由 `patch {}` 块启用）。
     */
    var patchEnabled = false

    /**
     * 是否注册批量端点（由 `batch {}` 块启用）。
     */
    var batchEnabled = false

    /**
     * 批量端点配置（由 `batch {}` 块修改）。
     */
    val batch = BatchConfig()

    /**
     * 自定义路由（由 `action {}` 块注册），在内置路由之后执行。
     */
    var customRoutes: (Route.() -> Unit)? = null
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
 * 顶层 `input {}`：同时配置 create / edit / patch（兼容原 api 语义）。
 */
fun <T : Any> ApiConfig<T>.input(block: EntityScope<T>.() -> Unit) {
    create.input(block)
    edit.input(block)
    patch.input(block)
}

/**
 * 顶层 `input(InputType::class) {}`：同时配置 create / edit / patch（兼容原 api 语义）。
 */
fun <T : Any, TInput : Input<T>> ApiConfig<T>.input(
    type: KClass<TInput>,
    block: InputScope<T, TInput>.() -> Unit,
) {
    create.input(type, block)
    edit.input(type, block)
    patch.input(type, block)
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
 * 启用 PATCH 路由并配置（PATCH 有独立配置；传入空块即按默认启用）。
 */
fun <T : Any> ApiConfig<T>.patch(block: EditConfig<T>.() -> Unit = {}) {
    patchEnabled = true
    this.patch.apply(block)
}

/**
 * 启用批量端点（`POST/PUT/DELETE {path}/batch`），复用 create/edit 配置。
 */
fun <T : Any> ApiConfig<T>.batch(block: BatchConfig.() -> Unit = {}) {
    batchEnabled = true
    this.batch.apply(block)
}

/**
 * 注册自定义路由（Ktor `Route` DSL），挂载到 `{path}` 下，在内置路由之后生效。
 *
 * ```
 * api<Book>("/book") {
 *     action {
 *         get("stats") { ... }
 *         post("{id}/publish") { ... }
 *     }
 * }
 * ```
 *
 * 注意：block 在注册期执行一次，handler 内捕获的外部状态为注册期快照（跨请求共享）。
 */
fun <T : Any> ApiConfig<T>.action(block: Route.() -> Unit) {
    customRoutes = block
}

/**
 * 批量端点配置。
 */
class BatchConfig {
    /**
     * 批量端点路径（相对 api 路径），默认 `batch`（即 `{path}/batch`）。
     */
    var path: String = Configuration.endpoint.batchPath

    /**
     * 是否注册批量创建端点。
     */
    var createEnabled: Boolean = true

    /**
     * 是否注册批量更新端点。
     */
    var updateEnabled: Boolean = true

    /**
     * 是否注册批量删除端点。
     */
    var deleteEnabled: Boolean = true

    /**
     * 批量删除的 id 参数名，默认 `ids`（`?ids=1,2,3`）。
     */
    var deleteIdsParameterName: String = Configuration.endpoint.batchIdsParameterName
}
