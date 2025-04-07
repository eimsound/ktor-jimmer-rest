package com.eimsound.ktor.route

import com.eimsound.ktor.config.Configuration
import com.eimsound.ktor.provider.*
import com.eimsound.ktor.validator.ValidationBuilder
import com.eimsound.util.ktor.Pager
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import org.babyfish.jimmer.Input

@KtorDsl
@JvmName("api")
inline fun <reified TEntity : Any> Route.api(
    path: String,
    pathVariable: String = Configuration.defaultPathVariable,
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
        entity = scope.entity
        validator = scope.validator
    }
    edit<TEntity> {
        val scope = ApiScope<TEntity>(call).apply { block() }
        entity = scope.entity
        validator = scope.validator
    }
    remove<TEntity>(pathVariable) {
        val scope = ApiScope<TEntity>(call).apply { block() }
        key = scope.key
    }
}

@KtorDsl
@JvmName("apiWithInput")
inline fun <reified TEntity : Any, reified TInput : Input<TEntity>> Route.api(
    path: String,
    pathVariable: String = Configuration.defaultPathVariable,
    crossinline block: suspend InputApiScope<TEntity, TInput>.() -> Unit,
) = route(path) {
    id<TEntity>(pathVariable) {
        val scope = InputApiScope<TEntity, TInput>(call).apply { block() }
        key = scope.key
        fetcher = scope.fetcher
    }
    list<TEntity> {
        val scope = InputApiScope<TEntity, TInput>(call).apply { block() }
        fetcher = scope.fetcher
        filter = scope.filter
        pager = scope.pager
    }
    create<TInput> {
        val scope = InputApiScope<TEntity, TInput>(call).apply { block() }
        entity = scope.entity
        validator = scope.validator
    }
    edit<TInput> {
        val scope = InputApiScope<TEntity, TInput>(call).apply { block() }
        entity = scope.entity
        validator = scope.validator
    }
    remove<TEntity>(pathVariable) {
        val scope = InputApiScope<TEntity, TInput>(call).apply { block() }
        key = scope.key
    }
}


class InputApiScope<T : Any, TInput : Input<T>>(override val call: RoutingCall) : QueryProvider<T>, ListProvider<T>,
    EditProvider<TInput>, CreateProvider<TInput>, RemoveProvider<T> {
    override var key: Any? = null
    override var entity: ((TInput) -> TInput)? = null
    override var validator: (ValidationBuilder.(TInput) -> Unit)? = null
    override var fetcher: Fetchers<T>? = null
    override var filter: (FilterScope<T>.() -> Unit)? = null
    override var pager: Pager = Pager()
}

class ApiScope<T : Any>(override val call: RoutingCall) : QueryProvider<T>, ListProvider<T>,
    EditProvider<T>, CreateProvider<T>, RemoveProvider<T> {
    override var key: Any? = null
    override var entity: ((T) -> T)? = null
    override var validator: (ValidationBuilder.(T) -> Unit)? = null
    override var fetcher: Fetchers<T>? = null
    override var filter: (FilterScope<T>.() -> Unit)? = null
    override var pager: Pager = Pager()
}
