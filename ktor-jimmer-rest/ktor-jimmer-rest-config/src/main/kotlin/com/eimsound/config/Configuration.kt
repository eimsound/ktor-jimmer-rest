package com.eimsound.ktor.jimmer.rest.config

import org.babyfish.jimmer.sql.kt.KSqlClient


object Configuration {
    var extParameterSeparator = "__"
    var subParameterSeparator = "_"

    var defaultPathVariable = "{id}"

    val page = PageConfiguration()

    lateinit var sqlClientFactory: () -> Lazy<KSqlClient>
}

