package com.eimsound.ktor.provider

import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.fetcher.FetcherCreator
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher


@FetcherDslMarker
interface FetcherProvider<T : Any> {
    var fetcher: Fetcher<T>?
}

@FetcherDslMarker
class FetcherScope<T : Any>(val creator: FetcherCreator<T>)

inline fun <reified T : Any> FetcherProvider<T>.fetcher(block: FetcherScope<T>.() -> Fetcher<T>) {
    fetcher = block(FetcherScope(newFetcher(T::class)))
}

@DslMarker
annotation class FetcherDslMarker
