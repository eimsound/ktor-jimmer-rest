package com.eimsound.ktor.jimmer.rest.provider

import com.eimsound.ktor.jimmer.rest.config.Configuration
import io.ktor.server.routing.RoutingCall
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery

class Pager {
    var enabled: Boolean = Configuration.page.enabledPage
    var pageIndex: Int = Configuration.page.defaultPageIndex
    var pageSize: Int = Configuration.page.defaultPageSize
    var pageIndexParameterName = Configuration.page.pageIndexParameterName
    var pageSizeParameterName = Configuration.page.pageSizeParameterName
}

interface PageProvider {
    var pager: Pager
}

fun <T : Any, R> KConfigurableRootQuery<T, R>.fetchPageOrElse(
    pager: Pager,
    elseBlock: KConfigurableRootQuery<T, R>.() -> List<R>
) =
    if (pager.enabled) {
        val pageFactory = Configuration.page.pageFactory
        if (pageFactory != null) {
            fetchPage(pager.pageIndex, pager.pageSize, pageFactory = pageFactory)
        } else {
            fetchPage(pager.pageIndex, pager.pageSize)
        }
    } else {
        elseBlock()
    }

inline fun PageProvider.pager(block: Pager.() -> Unit) {
    block(pager)
}
