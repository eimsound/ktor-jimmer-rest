package com.eimsound.ktor.provider


/**
 * 提供固定 key 的能力，作用于 `id`（GET /{id}）与 `remove`（DELETE /{id}）两条路由。
 *
 * 注意：一旦通过 [key] 设置了 key，它**优先于路径变量**，即 `GET /book/1` 会返回该 key
 * 对应的实体（即使路径里写的是 1）。典型场景是 id 来自上下文（如登录态、租户）而非路径；
 * 常规 CRUD 场景请勿使用，直接用路径变量即可。
 */
interface KeyProvider<T : Any> {
    var key: Any?
}

/**
 * 设置固定 key（会覆盖路径变量，同时影响 id 与 remove 路由），详见 [KeyProvider]。
 */
inline fun <reified T : Any> KeyProvider<T>.key(key: Any) {
    this.key = key
}



