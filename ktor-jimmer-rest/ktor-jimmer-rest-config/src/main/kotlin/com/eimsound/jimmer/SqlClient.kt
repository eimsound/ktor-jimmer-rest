package com.eimsound.jimmer

import com.eimsound.ktor.config.Configuration
import org.babyfish.jimmer.sql.kt.KSqlClient

val sqlClient: KSqlClient
    get() = Configuration.sqlClientFactory.invoke().value
