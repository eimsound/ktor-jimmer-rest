package com.eimsound.util.ktor

import com.eimsound.ktor.config.Configuration

class Pager {
    var enabled: Boolean = Configuration.pager.enabledPage
    var pageIndex: Int = Configuration.pager.defaultPageIndex
    var pageSize: Int = Configuration.pager.defaultPageSize
    var pageIndexParameterName = Configuration.pager.pageIndexParameterName
    var pageSizeParameterName = Configuration.pager.pageSizeParameterName
}
