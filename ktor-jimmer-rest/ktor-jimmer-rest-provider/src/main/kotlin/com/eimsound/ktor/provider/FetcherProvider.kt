package com.eimsound.ktor.provider

import org.babyfish.jimmer.View
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.fetcher.FetcherCreator
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import kotlin.reflect.KClass

sealed class Fetchers<T: Any>{
    data class Fetch<T: Any>(val fetcher: Fetcher<T>) : Fetchers<T>()
    data class ViewType<T: Any>(val viewType: KClass<out View<T>>) : Fetchers<T>()

}

@FetcherDslMarker
interface FetcherProvider<T : Any> {
    var fetcher: Fetchers<T>?
}


@FetcherDslMarker
class FetcherScope<T : Any>(val creator: FetcherCreator<T>)

inline fun <reified T : Any> FetcherProvider<T>.fetcher(block: FetcherScope<T>.() -> Fetcher<T>) {
    fetcher = Fetchers.Fetch<T>(block(FetcherScope(newFetcher(T::class))))
}

inline fun <reified T : Any> FetcherProvider<T>.fetcher(viewType: KClass<out View<T>>) {
    fetcher = Fetchers.ViewType<T>(viewType)
}

@DslMarker
annotation class FetcherDslMarker
