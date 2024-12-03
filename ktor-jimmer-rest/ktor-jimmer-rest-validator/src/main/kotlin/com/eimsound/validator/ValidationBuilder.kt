package com.eimsound.ktor.jimmer.rest.validator

import com.eimsound.validator.ValidationResult
import com.eimsound.validator.exception.ValidationExceptionCatcher
import java.time.Duration
import java.time.temporal.Temporal
import java.util.*
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.text.isNotBlank


open class ValidationBuilder(val entity: Any) {
    private val KProperty<*>.jsonName
        get() = name

    val errors = mutableListOf<String>()

    inline fun error(block: () -> String) = errors.add(block())

    private fun <T : Any> KProperty<T>.validateProperty(
        message: String? = null,
        predicate: T.() -> Boolean,
    ): KProperty<T> = apply {
        val value = runCatching {
            val value = entity::class.memberProperties.find { it.name == name }.let {
                it?.isAccessible = true
                it?.getter?.call(entity)
            }
            value
        }.getOrNull() as T?
        value.validate(jsonName, message, predicate)
    }


    fun <T : Any> T?.validate(
        name: String?,
        message: String? = null,
        predicate: T.() -> Boolean,
    ) = apply {
        if (this == null) {
            error { "$name cannot be null" }
        } else if (!predicate(this)) {
            error { message ?: "$name is invalid" }
        }
    }


    fun <T : Any> T?.notNull(name: String = "[field]", message: ((String?) -> String?)? = null): T? =
        this.validate(name, message?.invoke(name) ?: "$name cannot be null") {
            this != null
        }

    fun String?.notBlank(name: String = "[field]", message: ((String?) -> String?)? = null): String? =
        this.validate(name, message?.invoke(name) ?: "$name cannot be blank") {
            isNotBlank()
        }

    fun String?.length(
        name: String = "[field]",
        range: LongRange,
        message: ((String?) -> String?)? = null
    ): String? = this.validate(
        name, message?.invoke(name) ?: "$name must be between ${range.first} and ${range.last} characters long"
    ) {
        length in range
    }

    fun String?.length(name: String = "[field]", max: Long, message: ((String?) -> String?)? = null): String? =
        this.validate(message?.invoke(name) ?: "$name must be between 0 and $max characters long") {
            length in 0..max
        }

    fun String?.isUrl(name: String = "[field]", message: ((String?) -> String?)? = null): String? =
        this.validate(message?.invoke(name) ?: "$name must be a valid URL: https://example.com") {
            val urlRegex = """^https?://[^\s]+""".toRegex()
            urlRegex.matches(this)
        }

    fun String?.isUUID(name: String = "[field]", message: ((String?) -> String?)? = null): String? =
        this.validate(message?.invoke(name) ?: "$name must be a valid UUID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx") {
            UUID.fromString(this) != null
        }

    fun <T : Comparable<T>> T?.range(
        name: String = "[field]",
        range: ClosedRange<T>,
        message: ((String?) -> String?)? = null
    ): T? = this.validate(message?.invoke(name) ?: "$name must be between ${range.start} and ${range.endInclusive}") {
        this in range
    }

    fun <T : Comparable<T>> T?.max(name: String = "[field]", max: T, message: ((String?) -> String?)? = null): T? =
        this.validate(message?.invoke(name) ?: "$name must be less than $max") { this <= max }

    fun <T : Comparable<T>> T?.min(name: String = "[field]", min: T, message: ((String?) -> String?)? = null): T? =
        this.validate(message?.invoke(name) ?: "$name must be greater than $min") { this >= min }

    fun <T : Temporal> T?.before(name: String = "[field]", before: T, message: ((String?) -> String?)? = null): T? =
        this.validate(message?.invoke(name) ?: "$name must be before $before") {
            Duration.between(before, this).let { !it.isNegative || it.isZero }
        }

    fun <T : Temporal> T?.after(name: String = "[field]", after: T, message: ((String?) -> String?)? = null): T? =
        this.validate(message?.invoke(name) ?: "$name must be after $after") {
            Duration.between(after, this).let { it.isNegative || it.isZero }
        }

    fun <T : Temporal> T?.between(
        name: String = "[field]",
        from: T,
        to: T,
        message: ((String?) -> String?)? = null
    ): T? =
        this.validate(message?.invoke(name) ?: "$name must be between from $from and to $to") {
            Duration.between(from, this).let { !it.isNegative || it.isZero }
                && Duration.between(to, this).let { it.isNegative || it.isZero }
        }


    fun KProperty<String>.notBlank(message: String? = null): KProperty<String> =
        validateProperty(message ?: "$jsonName cannot be blank") {
            isNotBlank()
        }

    fun KProperty<String>.length(range: LongRange, message: String? = null): KProperty<String> =
        validateProperty(message ?: "$jsonName must be between ${range.first} and ${range.last} characters long") {
            length in range
        }

    fun KProperty<String>.length(max: Long, message: String? = null): KProperty<String> =
        validateProperty(message ?: "$jsonName must be between 0 and $max characters long") {
            length in 0..max
        }

    fun KProperty<String>.validUrl(message: String? = null): KProperty<String> =
        validateProperty(message ?: "$jsonName must be a valid URL: https://example.com") {
            val urlRegex = """^https?://[^\s]+""".toRegex()
            urlRegex.matches(this)
        }

    fun KProperty<String>.validUUID(message: String? = null): KProperty<String> =
        validateProperty(message ?: "$jsonName must be a valid UUID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx") {
            UUID.fromString(this) != null
        }

    fun <T : Comparable<T>> KProperty<T>.range(range: ClosedRange<T>, message: String? = null): KProperty<T> =
        validateProperty(message ?: "$jsonName must be between ${range.start} and ${range.endInclusive}") {
            this in range
        }

    fun <T : Comparable<T>> KProperty<T>.max(max: T, message: String? = null): KProperty<T> =
        validateProperty(message ?: "$jsonName must be less than $max") { this <= max }

    fun <T : Comparable<T>> KProperty<T>.min(min: T, message: String? = null): KProperty<T> =
        validateProperty(message ?: "$jsonName must be greater than $min") { this >= min }

    fun <T : Temporal> KProperty<T>.before(before: T, message: String? = null): KProperty<T> =
        validateProperty(message ?: "$jsonName must be before $before") {
            Duration.between(before, this).let { !it.isNegative || it.isZero }
        }

    fun <T : Temporal> KProperty<T>.after(after: T, message: String? = null): KProperty<T> =
        validateProperty(message ?: "$jsonName must be after $after") {
            Duration.between(after, this).let { it.isNegative || it.isZero }
        }

    fun <T : Temporal> KProperty<T>.between(from: T, to: T, message: String? = null): KProperty<T> =
        validateProperty(message ?: "$jsonName must be between from $from and to $to") {
            Duration.between(from, this).let { !it.isNegative || it.isZero }
                && Duration.between(to, this).let { it.isNegative || it.isZero }
        }
}


inline fun <T : Any> validate(entity: T, block: ValidationBuilder.(T) -> Unit): ValidationResult {
    val validationBuilder = ValidationBuilder(entity)
    val result = runCatching {
        validationBuilder.apply { block(entity) }
    }.getOrElse { e ->
        ValidationExceptionCatcher.of(e).handle(validationBuilder, e)
    }
    val errors = result.errors
    return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
}
