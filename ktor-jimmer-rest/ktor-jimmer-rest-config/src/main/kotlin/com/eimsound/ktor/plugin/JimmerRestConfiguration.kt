package com.eimsound.ktor.plugin

import com.eimsound.ktor.config.PagerConfiguration
import com.eimsound.ktor.config.ParserConfiguration
import com.eimsound.ktor.config.RouterConfiguration
import org.babyfish.jimmer.sql.kt.KSqlClient

class JimmerRestConfiguration {
    val pageConfiguration = PagerConfiguration()

    val parserConfiguration = ParserConfiguration()

    val routerConfiguration = RouterConfiguration()

    lateinit var jimmerSqlClientFactory: () -> Lazy<KSqlClient>
}

fun JimmerRestConfiguration.jimmerSqlClientFactory(block: () -> Lazy<KSqlClient>) {
    jimmerSqlClientFactory = block
}

fun JimmerRestConfiguration.pager(block: PagerConfiguration.() -> Unit) {
    block(pageConfiguration)
}

fun JimmerRestConfiguration.parser(block: ParserConfiguration.() -> Unit){
    block(parserConfiguration)
}

fun JimmerRestConfiguration.router(block: RouterConfiguration.() -> Unit) {
    block(routerConfiguration)
}
