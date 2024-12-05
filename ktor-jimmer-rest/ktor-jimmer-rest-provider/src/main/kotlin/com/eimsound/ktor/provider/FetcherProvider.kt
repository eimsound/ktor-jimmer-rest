package com.eimsound.ktor.provider

import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.fetcher.FetcherCreator
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher

interface FetcherProvider<T : Any> {
    var fetcher: Fetcher<T>?
}

inline fun <reified T : Any> FetcherProvider<T>.fetcher(block: FetcherCreator<T>.() -> Fetcher<T>) {
    fetcher = block(newFetcher(T::class))
}
