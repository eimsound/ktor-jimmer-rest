package com.eimsound.ktor.config

/**
 * 端点约定配置（路径与查询参数名等字面量）。
 */
class EndpointConfiguration {

    /**
     * 批量端点路径（相对 api 路径），默认 `batch`（即 `{path}/batch`）。
     */
    var batchPath: String = "batch"

    /**
     * 批量删除的 id 参数名，默认 `ids`（`?ids=1,2,3`）。
     */
    var batchIdsParameterName: String = "ids"

    /**
     * 动态排序参数名，默认 `sort`（`?sort=price,desc`）。
     */
    var sortParameterName: String = "sort"

    /**
     * 计数端点路径，默认 `count`（`GET {path}/count`）。
     */
    var countPath: String = "count"

    /**
     * 存在性判断端点路径，默认 `exists/{id}`（`GET {path}/exists/{id}`）。
     */
    var existsPath: String = "exists/{id}"
}
