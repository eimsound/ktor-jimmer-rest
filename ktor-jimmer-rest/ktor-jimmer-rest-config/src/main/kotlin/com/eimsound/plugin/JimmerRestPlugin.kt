package com.eimsound.ktor.jimmer.rest.plugin

import com.eimsound.ktor.jimmer.rest.config.Configuration
import io.ktor.server.application.*

val JimmerRest = createApplicationPlugin(name = "JimmerRestPlugin",
        createConfiguration = ::JimmerRestApplication) {
        Configuration.defaultPageSize = pluginConfig.defaultPageSize
        Configuration.defaultPageIndex = pluginConfig.defaultPageIndex
        Configuration.defaultEnabledPage = pluginConfig.defaultEnabledPage
        Configuration.defaultPathVariable = pluginConfig.defaultPathVariable
        Configuration.subParameterSeparator = pluginConfig.subParameterSeparator
        Configuration.parameterSeparator = pluginConfig.parameterSeparator
        Configuration.setJimmerClient(pluginConfig.jimmerSqlClient)
    }
