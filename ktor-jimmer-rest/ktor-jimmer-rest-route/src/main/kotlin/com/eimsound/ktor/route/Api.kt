package com.eimsound.ktor.route

import com.eimsound.ktor.config.Configuration
import com.eimsound.ktor.provider.*
import com.eimsound.ktor.validator.ValidationBuilder
import com.eimsound.util.ktor.Pager
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import org.babyfish.jimmer.sql.fetcher.Fetcher

@KtorDsl
inline fun <reified TEntity : Any> Route.api(
    path: String,
    pathVariable: String = Configuration.defaultPathVariable,
    crossinline block: suspend ApiProvider<TEntity>.() -> Unit,
) = route(path) {
    id<TEntity>(pathVariable){
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

interface ApiProvider<T : Any> :
    QueryProvider<T>, ListProvider<T>, EditProvider<T>, CreateProvider<T>, RemoveProvider<T>

class ApiScope<T : Any>(override val call: RoutingCall) : ApiProvider<T> {
    override var key: Any? = null
    override var entity: ((T) -> T)? = null
    override var validator: (ValidationBuilder.(T) -> Unit)? = null
    override var fetcher: Fetcher<T>? = null
    override var filter: (FilterScope<T>.() -> Unit)? = null
    override var pager: Pager = Pager()
}

