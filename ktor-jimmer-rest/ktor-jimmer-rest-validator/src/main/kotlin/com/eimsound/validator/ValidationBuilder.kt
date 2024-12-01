package com.eimsound.ktor.jimmer.rest.validator

import com.eimsound.validator.ValidationResult
import java.time.Duration
import java.time.temporal.Temporal
import java.util.*
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible



class ValidationBuilder(val entity: Any) {
    private val KProperty<*>.jsonName
        get() = name

    val errors = mutableListOf<String>()

    inline fun error(block: () -> String) = errors.add(block())

    fun <T : Any> KProperty<T>.validate(
        message: String? = null,
        predicate: T.() -> Boolean,
    ): KProperty<T> = apply {
        val value = runCatching {
            val value = entity::class.memberProperties.find { it.name == name }.let {
                it?.isAccessible = true
                it?.getter?.call(entity)
            }
            value
        }.getOrElse {
            null
        } as T?
        if (value != null && !value.predicate()) {
            error { message ?: "$jsonName is invalid" }
        }
    }


    fun KProperty<String>.notBlank(message: String? = null): KProperty<String> =
        validate(message ?: "$jsonName cannot be blank") { isNotBlank() }

    fun KProperty<String>.length(range: LongRange, message: String? = null): KProperty<String> =
        validate(message ?: "$jsonName must be between ${range.first} and ${range.last} characters long") {
            length in range
        }

    fun KProperty<String>.length(max: Long, message: String? = null): KProperty<String> =
        validate(message ?: "$jsonName must be between 0 and $max characters long") {
            length in 0..max
        }

    fun KProperty<String>.validUrl(message: String? = null): KProperty<String> =
        validate(message ?: "$jsonName must be a valid URL: https://example.com") {
            val urlRegex = """^https?://[^\s]+""".toRegex()
            urlRegex.matches(this)
        }

    fun KProperty<String>.validUUID(message: String? = null): KProperty<String> =
        validate(message ?: "$jsonName must be a valid UUID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx") {
            UUID.fromString(this) != null
        }

    fun <T : Comparable<T>> KProperty<T>.range(range: ClosedRange<T>, message: String? = null): KProperty<T> =
        validate(message ?: "$jsonName must be between ${range.start} and ${range.endInclusive}") {
            this in range
        }

    fun <T : Comparable<T>> KProperty<T>.max(max: T, message: String? = null): KProperty<T> =
        validate(message ?: "$jsonName must be less than $max") { this <= max }

    fun <T : Comparable<T>> KProperty<T>.min(min: T, message: String? = null): KProperty<T> =
        validate(message ?: "$jsonName must be greater than $min") { this >= min }

    fun <T : Temporal> KProperty<T>.before(before: T, message: String? = null): KProperty<T> =
        validate(message ?: "$jsonName must be before $before") {
            Duration.between(before, this).let { !it.isNegative || it.isZero }
        }

    fun <T : Temporal> KProperty<T>.after(after: T, message: String? = null): KProperty<T> =
        validate(message ?: "$jsonName must be after $after") {
            Duration.between(after, this).let { it.isNegative || it.isZero }
        }

    fun <T : Temporal> KProperty<T>.between(from: T, to: T, message: String? = null): KProperty<T> =
        validate(message ?: "$jsonName must be between from $from and to $to") {
            Duration.between(from, this).let { !it.isNegative || it.isZero }
                    && Duration.between(to, this).let { it.isNegative || it.isZero }
        }
}



inline fun <T : Any> validate(entity: T, block: ValidationBuilder.(T) -> Unit): ValidationResult {
    val validationBuilder = ValidationBuilder(entity).apply { block(entity) }
    val errors = validationBuilder.errors
    return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
}
