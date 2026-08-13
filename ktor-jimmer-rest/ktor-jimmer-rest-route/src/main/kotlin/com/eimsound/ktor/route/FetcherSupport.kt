package com.eimsound.ktor.route

import com.eimsound.ktor.provider.Fetchers
import org.babyfish.jimmer.sql.kt.KSqlClient

/**
 * 按 [Fetchers]（Fetcher DSL 或 View DTO）查询单个实体。
 */
@PublishedApi
internal fun <T : Any> KSqlClient.findById(fetchers: Fetchers<T>, id: Any): Any? =
    when (fetchers) {
        is Fetchers.Fetch<T> -> findById(fetchers.fetcher, id)
        is Fetchers.ViewType<T> -> findById(fetchers.viewType, id)
    }
