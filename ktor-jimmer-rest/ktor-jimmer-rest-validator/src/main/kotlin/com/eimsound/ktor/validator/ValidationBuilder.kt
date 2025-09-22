package com.eimsound.ktor.validator

import com.eimsound.ktor.validator.exception.catcher.ValidationExceptionCatcher
import java.time.Duration
import java.time.temporal.Temporal
import java.util.*
import kotlin.collections.isNotEmpty
import kotlin.ranges.contains
import kotlin.reflect.KProperty
import kotlin.text.isNotBlank

open class ValidationBuilder() {
    internal val errors = mutableListOf<String>()

    internal inline fun error(block: () -> String) = errors.add(block())

    private fun <T : Any> KProperty<T?>.validate(
        message: String,
        predicate: T.() -> Boolean,
    ): KProperty<T?> = apply {
        runCatching {
            val value = call()
            if (value == null || !predicate(value)) {
                error { message }
            }
        }.getOrElse { exception ->
            error { message }
        }
    }

    fun <T : Any> KProperty<T?>.validate(predicate: T.() -> Boolean, message: () -> String): KProperty<T?> =
        this@validate.validate(message(), predicate)

    fun <T : Any> KProperty<T?>.notNull(message: () -> String): KProperty<T?> =
        this@notNull.validate(message()) {
            this != null
        }

    fun KProperty<String?>.notBlank(message: () -> String): KProperty<String?> =
        this@notBlank.validate(message()) {
            isNotBlank()
        }

    fun <T> KProperty<Collection<T>?>.notEmpty(message: () -> String): KProperty<Collection<T>?>? =
        this@notEmpty.validate(message()) {
            isNotEmpty()
        }

    fun KProperty<String?>.length(
        range: LongRange,
        message: (LongRange) -> String
    ): KProperty<String?> = this@length.validate(
        message(range)
    ) {
        length in range
    }

    fun KProperty<String?>.length(max: Long, message: (Long) -> String): KProperty<String?> =
        this@length.validate(message(max)) {
            length in 0..max
        }

    fun KProperty<String?>.isUrl(message: () -> String): KProperty<String?> =
        this@isUrl.validate(message()) {
            val urlRegex = """^https?://[^\s]+""".toRegex()
            urlRegex.matches(this)
        }

    fun KProperty<String?>.isUUID(message: () -> String): KProperty<String?> =
        this@isUUID.validate(message()) {
            UUID.fromString(this) != null
        }

    fun KProperty<String?>.regex(regex: Regex, message: (Regex) -> String): KProperty<String?> =
        this@regex.validate(message(regex)) {
            regex.matches(this)
        }

    fun <T : Comparable<T>> KProperty<T?>.range(
        range: ClosedRange<T>,
        message: (ClosedRange<T>) -> String
    ): KProperty<T?> = this@range.validate(
        message(range)
    ) {
        this in range
    }

    fun <T : Comparable<T>> KProperty<T?>.max(max: T, message: (T) -> String): KProperty<T?> =
        this@max.validate(message(max)) { this <= max }

    fun <T : Comparable<T>> KProperty<T?>.min(min: T, message: (T) -> String): KProperty<T?> =
        this@min.validate(message(min)) { this >= min }

    fun <T : Temporal> KProperty<T?>.before(before: T, message: (T) -> String): KProperty<T?> =
        this@before.validate(message(before)) {
            Duration.between(before, this).let { !it.isNegative || it.isZero }
        }

    fun <T : Temporal> KProperty<T?>.after(after: T, message: (T) -> String): KProperty<T?> =
        this@after.validate(message(after)) {
            Duration.between(after, this).let { it.isNegative || it.isZero }
        }

    fun <T : Temporal> KProperty<T?>.between(
        from: T,
        to: T,
        message: (T, T) -> String
    ): KProperty<T?> = this@between.validate(message(from, to)) {
        Duration.between(from, this).let { !it.isNegative || it.isZero }
            && Duration.between(to, this).let { it.isNegative || it.isZero }
    }
}


fun <T : Any, V : ValidationBuilder> V.validate(body: T, block: V.(T) -> Unit): ValidationResult {
    val result = runCatching {
        this.apply { block(body) }
    }.getOrElse { e ->
        ValidationExceptionCatcher.of(e).handle(this, e, "Validation failed: ${e.message}")
    }
    val errors = result.errors
    return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
}
