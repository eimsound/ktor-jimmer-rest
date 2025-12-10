package com.eimsound.util.parser

import com.eimsound.ktor.config.Configuration
import io.ktor.http.parsing.ParseException
import java.math.BigDecimal
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
fun <T : Any> String.parse(type: KClass<T>): T = runCatching {
    when (type) {
        String::class -> this
        Int::class -> this.toInt()
        Long::class -> this.toLong()
        Float::class -> this.toFloat()
        Double::class -> this.toDouble()
        Boolean::class -> this.toBoolean()
        BigDecimal::class -> this.toBigDecimal()
        Char::class -> this[0]
        Short::class -> this.toShort()
        ByteArray::class -> this.toByteArray()
        else -> Configuration.parser.parsers.getOrElse(type) {
            throw ParseException("Unsupported type: ${type.simpleName}")
        }.invoke(this)
    } as T
}.getOrElse { exception ->
    throw ParseException("$this is not a valid value", exception)
}
