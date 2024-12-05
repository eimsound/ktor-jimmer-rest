package com.eimsound.ktor.validator.exception.catcher

import com.eimsound.ktor.validator.ValidationBuilder
import org.babyfish.jimmer.UnloadedException
import kotlin.reflect.KClass

interface ValidationExceptionCatcher<out T : Throwable> {
    fun handle(builder: ValidationBuilder, e: @UnsafeVariance T): ValidationBuilder

    companion object {

        val catchers = mutableMapOf<KClass<out Throwable>, ValidationExceptionCatcher<*>>()

        fun <T : Throwable, E : ValidationExceptionCatcher<T>> registerValidationExceptionCatcher(
            e: KClass<T>,
            catcher: E
        ) {
            catchers += e to catcher
        }

        init {
            registerValidationExceptionCatcher(
                UnloadedException::class, ValidationUnloadedExceptionCatcher()
            )
        }

        fun of(e: Throwable): ValidationExceptionCatcher<*> = catchers.getOrElse(e::class) {
            throw e
        }
    }
}


