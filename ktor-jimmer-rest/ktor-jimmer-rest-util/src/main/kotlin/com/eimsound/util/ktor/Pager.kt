package com.eimsound.util.ktor

import com.eimsound.ktor.config.Configuration

class Pager {
    var enabled: Boolean = Configuration.page.enabledPage
    var pageIndex: Int = Configuration.page.defaultPageIndex
    var pageSize: Int = Configuration.page.defaultPageSize
    var pageIndexParameterName = Configuration.page.pageIndexParameterName
    var pageSizeParameterName = Configuration.page.pageSizeParameterName
}
