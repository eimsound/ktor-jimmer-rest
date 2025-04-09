package com.eimsound.util.ktor

import com.eimsound.ktor.config.Configuration


/**
 * 一个扩展的参数类型，用于解析url参数
 *
 * 例如 createUser__ge  createUser__le
 *
 * @property name 参数名
 * @property ext  扩展名
 * @property value  参数值
 */
class Parameter<T>(val name: String) {
    // 比如 ge le exact
    var ext: String? = null
    val separator get() = Configuration.router.extParameterSeparator
    val hasExt get() = ext != null
    // 比如 createTime__ge createTime__le name__exact
    val nameWithExt: String get() = name + if (hasExt) separator + ext else ""

    var value: T? = null
}
