package com.eimsound.ktor.jimmer.rest.plugin

import com.eimsound.ktor.jimmer.rest.config.Configuration
import io.ktor.server.application.*

val JimmerRest = createApplicationPlugin(
    name = "JimmerRestPlugin",
    createConfiguration = ::JimmerRestConfiguration
) {
    Configuration.page.defaultPageSize = pluginConfig.pageConfiguration.defaultPageSize
    Configuration.page.defaultPageIndex = pluginConfig.pageConfiguration.defaultPageIndex
    Configuration.page.enabledPage = pluginConfig.pageConfiguration.enabledPage
    Configuration.page.pageIndexParameterName = pluginConfig.pageConfiguration.pageIndexParameterName
    Configuration.page.pageSizeParameterName = pluginConfig.pageConfiguration.pageSizeParameterName
    Configuration.page.pageFactory = pluginConfig.pageConfiguration.pageFactory

    Configuration.defaultPathVariable = pluginConfig.defaultPathVariable
    Configuration.subParameterSeparator = pluginConfig.subParameterSeparator
    Configuration.extParameterSeparator = pluginConfig.extParameterSeparator

    Configuration.sqlClientFactory = pluginConfig.jimmerSqlClientFactory
}
