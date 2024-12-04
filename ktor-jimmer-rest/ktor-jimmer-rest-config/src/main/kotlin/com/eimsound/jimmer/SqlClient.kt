package com.eimsound.ktor.jimmer.rest.jimmer

import com.eimsound.ktor.jimmer.rest.config.Configuration
import org.babyfish.jimmer.sql.kt.KSqlClient

val sqlClient: KSqlClient
    get() = Configuration.sqlClientFactory?.invoke()?.value
        ?: throw IllegalArgumentException("sqlClientFactory is not initialized")
