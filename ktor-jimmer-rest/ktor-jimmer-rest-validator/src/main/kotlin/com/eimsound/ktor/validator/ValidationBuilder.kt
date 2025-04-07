package com.eimsound.ktor.validator

import com.eimsound.ktor.validator.exception.catcher.ValidationExceptionCatcher
import java.time.Duration
import java.time.temporal.Temporal
import java.util.*
import kotlin.text.isNotBlank

open class ValidationBuilder() {
    val errors = mutableListOf<String>()

    inline fun error(block: () -> String) = errors.add(block())

    fun <T : Any> T?.validateValue(
        message: String,
        predicate: T.() -> Boolean,
    ) = apply {
        if (this == null || !predicate()) {
            error { message }
        }
    }

    fun String?.notBlank(message: () -> String): String? =
        validateValue(message()) {
            isNotBlank()
        }

    fun <T> Collection<T>?.notEmpty(message: () -> String): Collection<T>? =
        validateValue(message()) {
            isNotEmpty()
        }

    fun String?.length(
        range: LongRange,
        message: (LongRange) -> String
    ): String? = validateValue(
        message(range)
    ) {
        length in range
    }

    fun String?.length(max: Long, message: (Long) -> String): String? =
        validateValue(message(max)) {
            length in 0..max
        }

    fun String?.isUrl(message: () -> String): String? =
        validateValue(message()) {
            val urlRegex = """^https?://[^\s]+""".toRegex()
            urlRegex.matches(this)
        }

    fun String?.isUUID(message: () -> String): String? =
        validateValue(message()) {
            UUID.fromString(this) != null
        }

    fun String?.regex(regex: Regex, message: (Regex) -> String): String? =
        validateValue(message(regex)) {
            regex.matches(this)
        }

    fun <T : Comparable<T>> T?.range(
        range: ClosedRange<T>,
        message: (ClosedRange<T>) -> String
    ): T? = validateValue(
        message(range)
    ) {
        this in range
    }

    fun <T : Comparable<T>> T?.max(max: T, message: (T) -> String): T? =
        validateValue(message(max)) { this <= max }

    fun <T : Comparable<T>> T?.min(min: T, message: (T) -> String): T? =
        validateValue(message(min)) { this >= min }

    fun <T : Temporal> T?.before(before: T, message: (T) -> String): T? =
        validateValue(message(before)) {
            Duration.between(before, this).let { !it.isNegative || it.isZero }
        }

    fun <T : Temporal> T?.after(after: T, message: (T) -> String): T? =
        validateValue(message(after)) {
            Duration.between(after, this).let { it.isNegative || it.isZero }
        }

    fun <T : Temporal> T?.between(
        from: T,
        to: T,
        message: (T, T) -> String
    ): T? = validateValue(message(from, to)) {
            Duration.between(from, this).let { !it.isNegative || it.isZero }
                && Duration.between(to, this).let { it.isNegative || it.isZero }
        }
}


inline fun <reified T : Any> validate(body: T, block: ValidationBuilder.(T) -> Unit): ValidationResult {
    val validationBuilder = ValidationBuilder()
    val result = runCatching {
        validationBuilder.apply { block(body) }
    }.getOrElse { e ->
        ValidationExceptionCatcher.of(e).handle(validationBuilder, e)
    }
    val errors = result.errors
    return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
}
