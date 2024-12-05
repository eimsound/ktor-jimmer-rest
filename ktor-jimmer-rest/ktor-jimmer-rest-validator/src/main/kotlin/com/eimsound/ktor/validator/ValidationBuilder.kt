package com.eimsound.ktor.validator

import com.eimsound.util.jimmer.getPropertyFullName
import com.eimsound.ktor.validator.exception.catcher.ValidationExceptionCatcher
import java.time.Duration
import java.time.temporal.Temporal
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.text.isNotBlank

open class ValidationBuilder(val klass: KClass<*>) {

    private val KProperty<*>.propertyName
        get() = getPropertyFullName(this, klass)

    val errors = mutableListOf<String>()

    inline fun error(block: () -> String) = errors.add(block())

    private fun <T : Any> KProperty<T?>.validateProperty(
        message: String? = null,
        predicate: T.() -> Boolean,
    ): KProperty<T?> = apply {
        val value = runCatching {
            this?.call() ?: null
        }.getOrElse { exception -> null }
        value.validateValue(propertyName, message, predicate)
    }

    fun <T : Any> T?.validateValue(
        name: String,
        message: String? = null,
        predicate: T.() -> Boolean,
    ) = apply {
        if (this == null) {
            error { "$name cannot be null" }
        } else if (!predicate(this)) {
            error { message ?: "$name is invalid" }
        }
    }

    fun String?.notBlank(name: String, message: ((String) -> String?)? = null): String? =
        validateValue(name, message?.invoke(name) ?: "$name cannot be blank") {
            isNotBlank()
        }

    fun <T> Collection<T>?.notEmpty(name: String, message: ((String) -> String?)? = null): Collection<T>? =
        validateValue(name, message?.invoke(name) ?: "$name cannot be empty") {
            isNotEmpty()
        }

    fun String?.length(
        name: String,
        range: LongRange,
        message: ((String, LongRange) -> String?)? = null
    ): String? = validateValue(
        name, message?.invoke(name, range) ?: "$name must be between ${range.first} and ${range.last} characters long"
    ) {
        length in range
    }

    fun String?.length(name: String, max: Long, message: ((String, Long) -> String?)? = null): String? =
        validateValue(message?.invoke(name, max) ?: "$name must be between 0 and $max characters long") {
            length in 0..max
        }

    fun String?.isUrl(name: String, message: ((String) -> String?)? = null): String? =
        validateValue(message?.invoke(name) ?: "$name must be a valid URL: https://example.com") {
            val urlRegex = """^https?://[^\s]+""".toRegex()
            urlRegex.matches(this)
        }

    fun String?.isUUID(name: String, message: ((String) -> String?)? = null): String? =
        validateValue(message?.invoke(name) ?: "$name must be a valid UUID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx") {
            UUID.fromString(this) != null
        }

    fun String?.regex(name: String, regex: Regex, message: ((String, Regex) -> String?)? = null): String? =
        validateValue(message?.invoke(name, regex) ?: "$name is regex invalid: ${regex.pattern}") {
            regex.matches(this)
        }

    fun <T : Comparable<T>> T?.range(
        name: String,
        range: ClosedRange<T>,
        message: ((String, ClosedRange<T>) -> String?)? = null
    ): T? = validateValue(
        message?.invoke(name, range) ?: "$name must be between ${range.start} and ${range.endInclusive}"
    ) {
        this in range
    }

    fun <T : Comparable<T>> T?.max(name: String, max: T, message: ((String, T) -> String?)? = null): T? =
        validateValue(message?.invoke(name, max) ?: "$name must be less than $max") { this <= max }

    fun <T : Comparable<T>> T?.min(name: String, min: T, message: ((String, T) -> String?)? = null): T? =
        validateValue(message?.invoke(name, min) ?: "$name must be greater than $min") { this >= min }

    fun <T : Temporal> T?.before(name: String, before: T, message: ((String, T) -> String?)? = null): T? =
        validateValue(message?.invoke(name, before) ?: "$name must be before $before") {
            Duration.between(before, this).let { !it.isNegative || it.isZero }
        }

    fun <T : Temporal> T?.after(name: String, after: T, message: ((String, T) -> String?)? = null): T? =
        validateValue(message?.invoke(name, after) ?: "$name must be after $after") {
            Duration.between(after, this).let { it.isNegative || it.isZero }
        }

    fun <T : Temporal> T?.between(
        name: String,
        from: T,
        to: T,
        message: ((String, T, T) -> String?)? = null
    ): T? =
        validateValue(message?.invoke(name, from, to) ?: "$name must be between from $from and to $to") {
            Duration.between(from, this).let { !it.isNegative || it.isZero }
                && Duration.between(to, this).let { it.isNegative || it.isZero }
        }

    // ------------------------ KProperty ------------------------

    fun KProperty<String?>.notBlank(message: ((String) -> String?)? = null): KProperty<String?> =
        validateProperty(message?.invoke(propertyName) ?: "$propertyName cannot be blank") {
            isNotBlank()
        }

    fun <T> KProperty<Collection<T>?>.notEmpty(message: ((String) -> String?)? = null): KProperty<Collection<T>?> =
        validateProperty(message?.invoke(propertyName) ?: "$propertyName cannot be empty") {
            isNotEmpty()
        }

    fun KProperty<String?>.length(
        range: LongRange,
        message: ((String, LongRange) -> String?)? = null
    ): KProperty<String?> =
        validateProperty(
            message?.invoke(propertyName, range)
                ?: "$propertyName must be between ${range.first} and ${range.last} characters long"
        ) {
            length in range
        }

    fun KProperty<String?>.length(max: Long, message: ((String, Long) -> String?)? = null): KProperty<String?> =
        validateProperty(
            message?.invoke(propertyName, max) ?: "$propertyName must be between 0 and $max characters long"
        ) {
            length in 0..max
        }

    fun KProperty<String?>.isUrl(message: ((String) -> String?)? = null): KProperty<String?> =
        validateProperty(message?.invoke(propertyName) ?: "$propertyName must be a valid URL: https://example.com") {
            val urlRegex = """^https?://[^\s]+""".toRegex()
            urlRegex.matches(this)
        }

    fun KProperty<String?>.isUUID(message: ((String) -> String?)? = null): KProperty<String?> =
        validateProperty(
            message?.invoke(propertyName) ?: "$propertyName must be a valid UUID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
        ) {
            UUID.fromString(this) != null
        }

    fun KProperty<String?>.regex(regex: Regex, message: ((String, Regex) -> String?)? = null): KProperty<String?> =
        validateProperty(message?.invoke(propertyName, regex) ?: "$propertyName is regex invalid: ${regex.pattern}") {
            regex.matches(this)
        }

    fun <T : Comparable<T>> KProperty<T?>.range(
        range: ClosedRange<T>,
        message: ((String, ClosedRange<T>) -> String?)? = null
    ): KProperty<T?> =
        validateProperty(
            message?.invoke(propertyName, range)
                ?: "$propertyName must be between ${range.start} and ${range.endInclusive}"
        ) {
            this in range
        }

    fun <T : Comparable<T>> KProperty<T?>.max(max: T, message: ((String, T) -> String?)? = null): KProperty<T?> =
        validateProperty(message?.invoke(propertyName, max) ?: "$propertyName must be less than $max") { this <= max }

    fun <T : Comparable<T>> KProperty<T?>.min(min: T, message: ((String, T) -> String?)? = null): KProperty<T?> =
        validateProperty(
            message?.invoke(propertyName, min) ?: "$propertyName must be greater than $min"
        ) { this >= min }

    fun <T : Temporal> KProperty<T?>.before(before: T, message: ((String, T) -> String?)? = null): KProperty<T?> =
        validateProperty(message?.invoke(propertyName, before) ?: "$propertyName must be before $before") {
            Duration.between(before, this).let { !it.isNegative || it.isZero }
        }

    fun <T : Temporal> KProperty<T?>.after(after: T, message: ((String, T) -> String?)? = null): KProperty<T?> =
        validateProperty(message?.invoke(propertyName, after) ?: "$propertyName must be after $after") {
            Duration.between(after, this).let { it.isNegative || it.isZero }
        }

    fun <T : Temporal> KProperty<T?>.between(
        from: T,
        to: T,
        message: ((String, T, T) -> String?)? = null
    ): KProperty<T?> =
        validateProperty(
            message?.invoke(propertyName, from, to) ?: "$propertyName must be between from $from and to $to"
        ) {
            Duration.between(from, this).let { !it.isNegative || it.isZero }
                && Duration.between(to, this).let { it.isNegative || it.isZero }
        }
}


inline fun <reified T : Any> validate(body: T, block: ValidationBuilder.(T) -> Unit): ValidationResult {
    val validationBuilder = ValidationBuilder(T::class)
    val result = runCatching {
        validationBuilder.apply { block(body) }
    }.getOrElse { e ->
        ValidationExceptionCatcher.of(e).handle(validationBuilder, e)
    }
    val errors = result.errors
    return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
}
