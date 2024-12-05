package com.eimsound.ktor.jimmer.rest.util.reflect.jimmer

import com.eimsound.ktor.jimmer.rest.util.reflect.getPropertyOwner
import com.eimsound.ktor.jimmer.rest.util.reflect.getPropertyTypeByAnnotation
import org.babyfish.jimmer.sql.Id
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

inline fun <T> getPropertyFullName(property: KProperty<T>, bound: KClass<*>): String {
    val propertyOwner = getPropertyOwner(property)
    if (propertyOwner == bound) {
        return property.name
    }
    val simpleName = propertyOwner.simpleName
    return "$simpleName.${property.name}"
}

inline fun <reified T : Any> entityIdType(): KClass<*> = getPropertyTypeByAnnotation<T>(Id::class)
