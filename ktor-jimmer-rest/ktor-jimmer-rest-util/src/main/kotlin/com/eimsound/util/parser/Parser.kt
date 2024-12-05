package com.eimsound.util.parser

import java.math.BigDecimal
import kotlin.reflect.KClass

inline fun String.parse(type: KClass<*>) = when (type) {
    Int::class -> this.toInt()
    Long::class -> this.toLong()
    Float::class -> this.toFloat()
    Double::class -> this.toDouble()
    Boolean::class -> this.toBoolean()
    BigDecimal::class -> this.toBigDecimal()
    Char::class -> this[0]
    Short::class -> this.toShort()
    ByteArray::class -> this.toByteArray()
    else -> this
}
