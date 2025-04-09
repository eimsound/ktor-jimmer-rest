package com.eimsound.ktor.config

import org.babyfish.jimmer.sql.ast.impl.query.PageSource

class PagerConfiguration{
    var defaultPageIndex: Int = 0
    var defaultPageSize: Int = 10
    var enabledPage: Boolean = true
    var pageIndexParameterName: String = "pageIndex"
    var pageSizeParameterName: String = "pageSize"
    var pageFactory: ((List<*>, Long, PageSource) -> Any)? = null
}
