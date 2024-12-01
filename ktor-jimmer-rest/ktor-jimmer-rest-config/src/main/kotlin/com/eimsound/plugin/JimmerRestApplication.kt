package com.eimsound.ktor.jimmer.rest.plugin

import org.babyfish.jimmer.sql.kt.KSqlClient

class JimmerRestApplication {
    var parameterSeparator = "__"
    var subParameterSeparator = "_"
    var defaultPageIndex = 0
    var defaultPageSize = 10
    var defaultEnabledPage = true
    var defaultPathVariable = "{id}"
    lateinit var jimmerSqlClient: KSqlClient
}