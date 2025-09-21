package com.eimsound.util.jimmer

import com.eimsound.ktor.config.Configuration
import com.eimsound.util.ktor.Pager
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableRootQuery
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable

inline fun <T : Any, R> KConfigurableRootQuery<KNonNullTable<T>, R>.fetchPageOrElse(
    pager: Pager,
    elseBlock: KConfigurableRootQuery<KNonNullTable<T>, R>.() -> List<Any>
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
