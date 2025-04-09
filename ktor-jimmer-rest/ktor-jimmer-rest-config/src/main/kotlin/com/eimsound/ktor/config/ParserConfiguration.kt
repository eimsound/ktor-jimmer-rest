package com.eimsound.ktor.config

import kotlin.reflect.KClass

class ParserConfiguration {
    val parsers = mutableMapOf<KClass<*>, (String) -> Any>()
    inline fun <reified T : Any> register(noinline parser: String.() -> T) {
        parsers += T::class to parser
    }
}
