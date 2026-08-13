package com.eimsound.ktor.config

import org.babyfish.jimmer.sql.kt.KSqlClient


object Configuration {
    val router = RouterConfiguration()

    val parser = ParserConfiguration()

    val pager = PagerConfiguration()

    val endpoint = EndpointConfiguration()

    lateinit var sqlClientFactory: () -> Lazy<KSqlClient>
}
