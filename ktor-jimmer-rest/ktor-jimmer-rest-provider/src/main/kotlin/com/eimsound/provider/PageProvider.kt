package com.eimsound.ktor.jimmer.rest.provider

import com.eimsound.ktor.jimmer.rest.config.Configuration
class Page(
    var enabled: Boolean = Configuration.defaultEnabledPage,
    var pageIndex: Int = Configuration.defaultPageIndex,
    var pageSize: Int = Configuration.defaultPageSize,
)

interface PageProvider {
    var page: Page
}

inline fun PageProvider.page(block: Page.() -> Unit) {
    block(page)
}
