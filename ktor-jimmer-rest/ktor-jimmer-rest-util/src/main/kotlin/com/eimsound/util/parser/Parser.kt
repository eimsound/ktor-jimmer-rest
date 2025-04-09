package com.eimsound.util.parser

import com.eimsound.ktor.config.Configuration
import java.math.BigDecimal
import kotlin.reflect.KClass

inline fun <T : Any> String.parse(type: KClass<T>): T = when (type) {
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
        throw ClassCastException("Unsupported type: ${type.simpleName}")
    }.invoke(this)
} as T
