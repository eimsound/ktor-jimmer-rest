package com.eimsound.ktor.route

import com.eimsound.ktor.config.Configuration
import com.eimsound.ktor.provider.*
import com.eimsound.util.ktor.Pager
import io.ktor.server.routing.*
import io.ktor.utils.io.*

@KtorDsl
@JvmName("api")
inline fun <reified TEntity : Any> Route.api(
    path: String,
    pathVariable: String = Configuration.router.defaultPathVariable,
    crossinline block: suspend ApiScope<TEntity>.() -> Unit,
) = route(path) {
    id<TEntity>(pathVariable) {
        val scope = ApiScope<TEntity>(call).apply { block() }
        key = scope.key
        fetcher = scope.fetcher
    }
    list<TEntity> {
        val scope = ApiScope<TEntity>(call).apply { block() }
        fetcher = scope.fetcher
        filter = scope.filter
        pager = scope.pager
    }
    create<TEntity> {
        val scope = ApiScope<TEntity>(call).apply { block() }
        input = scope.input
        validator = scope.validator
        transformer = scope.transformer
    }
    edit<TEntity> {
        val scope = ApiScope<TEntity>(call).apply { block() }
        input = scope.input
        validator = scope.validator
        transformer = scope.transformer
    }
    remove<TEntity>(pathVariable) {
        val scope = ApiScope<TEntity>(call).apply { block() }
        key = scope.key
    }
}

@DslMarker
annotation class ApiDslMarker

@ApiDslMarker
class ApiScope<T : Any>(override val call: RoutingCall) : QueryProvider<T>, ListProvider<T>,
    EditProvider<T>, CreateProvider<T>, RemoveProvider<T> {
    override var key: Any? = null
    override var input: Inputs<T> = Inputs.Entity()
    override var validator: Validators<T>? = null
    override var fetcher: Fetchers<T>? = null
    override var filter: Filters<T>? = null
    override var transformer: Transformers<T>? = null
    override var pager: Pager = Pager()
}
