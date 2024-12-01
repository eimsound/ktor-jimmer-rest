package com.eimsound.ktor.jimmer.rest.config

import org.babyfish.jimmer.sql.kt.KSqlClient

lateinit var sqlClient: KSqlClient


object Configuration {
    var parameterSeparator = "__"
    var subParameterSeparator = "_"
    var defaultPageIndex = 0
    var defaultPageSize = 10
    var defaultEnabledPage = true
    var defaultPathVariable = "{id}"



    internal fun setJimmerClient(client: KSqlClient){
        sqlClient = client
    }
}

