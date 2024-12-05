package com.eimsound.ktor.plugin

import com.eimsound.ktor.config.PageConfiguration
import org.babyfish.jimmer.sql.kt.KSqlClient

class JimmerRestConfiguration {
    var extParameterSeparator = "__"
    var subParameterSeparator = "_"

    var defaultPathVariable = "{id}"

    var pageConfiguration = PageConfiguration()

    lateinit var jimmerSqlClientFactory: () -> Lazy<KSqlClient>
}

fun JimmerRestConfiguration.jimmerSqlClientFactory(block: () -> Lazy<KSqlClient>) {
    jimmerSqlClientFactory = block
}

fun JimmerRestConfiguration.pageConfiguration(block: (PageConfiguration).() -> Unit) {
    block(pageConfiguration)
}
