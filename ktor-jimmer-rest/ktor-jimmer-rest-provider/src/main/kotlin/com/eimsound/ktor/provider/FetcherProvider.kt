package com.eimsound.ktor.provider

import com.eimsound.ktor.provider.Fetchers.Fetch
import com.eimsound.ktor.provider.Fetchers.ViewType
import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import org.babyfish.jimmer.sql.kt.fetcher.FetcherCreator
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import kotlin.reflect.KClass


@DslMarker
annotation class FetcherDslMarker

sealed class Fetchers<T : Any> {
    data class Fetch<T : Any>(val fetcher: Fetcher<T>) : Fetchers<T>()
    data class ViewType<T : Any>(val viewType: KClass<out View<T>>) : Fetchers<T>()
}

operator inline fun <T : Any> Fetchers<T>?.invoke(table: KNonNullTable<T>) = this?.run {
    when (this) {
        is ViewType -> table.fetch(viewType)
        is Fetch -> table.fetch(fetcher)
    }
}

interface FetcherProvider<T : Any> {
    var fetcher: Fetchers<T>?
}


@FetcherDslMarker
class FetcherScope<T : Any>(val fetch: FetcherCreator<T>)

inline fun <reified T : Any> FetcherProvider<T>.fetcher(block: FetcherScope<T>.() -> Fetcher<T>) {
    fetcher = Fetchers.Fetch(block(FetcherScope(newFetcher(T::class))))
}

inline fun <reified T : Any> FetcherProvider<T>.fetcher(viewType: KClass<out View<T>>) {
    fetcher = Fetchers.ViewType(viewType)
}
