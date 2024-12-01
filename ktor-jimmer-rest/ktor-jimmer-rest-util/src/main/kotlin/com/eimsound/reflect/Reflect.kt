package com.eimsound.ktor.jimmer.rest.util.reflect

import java.math.BigDecimal
import kotlin.jvm.internal.PropertyReference0Impl
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

inline fun <reified T : Any> getPropertyByAnnotation(annotation: KClass<out Annotation>) =
    T::class.memberProperties.find { it.annotations.any { it.annotationClass == annotation } }

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

fun getPropertyByPropertyName(type: KClass<*>, name: String): KProperty<*>? =
    type.memberProperties.find { it.name == name } as KProperty<*>

fun getMemberByMemberName(type: KClass<*>, name: String): KCallable<*>? =
    type.members.find { it.name == name }

inline fun <reified TClass : Any, reified TProperty> getPropertyByPropertyName(name: String)
    : KProperty<TProperty>? =
    TClass::class.memberProperties.find { it.name == name } as? KProperty<TProperty>

inline fun <reified TClass : Any, reified TProperty : Any> getTypeByPropertyName(name: String) =
    getPropertyByPropertyName<TClass, TProperty>(name)?.returnType?.classifier as KClass<*>

inline fun <reified T : Any> getTypeByAnnotation(annotation: KClass<out Annotation>) =
    getPropertyByAnnotation<T>(annotation)?.returnType?.classifier as KClass<*>

inline fun getPropertyReceiver(property :KProperty<*>) = (property as PropertyReference0Impl).boundReceiver

