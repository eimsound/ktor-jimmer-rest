package com.eimsound.util.jimmer

import com.eimsound.ktor.config.Configuration
import com.eimsound.util.ktor.Pager
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery

fun <T : Any, R> KConfigurableRootQuery<T, R>.fetchPageOrElse(
    pager: Pager,
    elseBlock: KConfigurableRootQuery<T, R>.() -> List<Any>
) =
    if (pager.enabled) {
        val pageFactory = Configuration.pager.pageFactory
        if (pageFactory != null) {
            fetchPage(pager.pageIndex, pager.pageSize, pageFactory = pageFactory)
        } else {
            fetchPage(pager.pageIndex, pager.pageSize)
        }
    } else {
        elseBlock()
    }
