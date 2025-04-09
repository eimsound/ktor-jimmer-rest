package com.eimsound.ktor.plugin

import com.eimsound.ktor.config.Configuration
import io.ktor.server.application.*

val JimmerRest = createApplicationPlugin(
    name = "JimmerRestPlugin",
    createConfiguration = ::JimmerRestConfiguration
) {
    val pageConfiguration = pluginConfig.pageConfiguration
    Configuration.pager.apply{
        defaultPageIndex = pageConfiguration.defaultPageIndex
        defaultPageSize = pageConfiguration.defaultPageSize
        enabledPage = pageConfiguration.enabledPage
        pageIndexParameterName = pageConfiguration.pageIndexParameterName
        pageSizeParameterName = pageConfiguration.pageSizeParameterName
        pageFactory = pageConfiguration.pageFactory
    }
    val parserConfiguration = pluginConfig.parserConfiguration
    Configuration.parser.apply {
        parsers.putAll(parserConfiguration.parsers)
    }
    val routerConfiguration = pluginConfig.routerConfiguration
    Configuration.router.apply{
        extParameterSeparator = routerConfiguration.extParameterSeparator
        subParameterSeparator = routerConfiguration.subParameterSeparator
        defaultPathVariable = routerConfiguration.defaultPathVariable
    }
    Configuration.sqlClientFactory = pluginConfig.jimmerSqlClientFactory
}
