package com.eimsound.util.jimmer

import com.eimsound.util.reflect.getMemberByMemberName
import com.eimsound.util.reflect.getPropertyByPropertyName
import com.eimsound.util.reflect.getPropertyOwner
import com.eimsound.util.reflect.getPropertyTypeByAnnotation
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

inline fun tableName(receiver: Any): String {
    val javaTableName = getPropertyByPropertyName(receiver::class, "javaTable")?.getter?.call(receiver).toString()
    return javaTableName
}

inline fun tableType(receiver: Any): String {
    val typeName = getMemberByMemberName(receiver::class, "getImmutableType")?.call(receiver).toString()
    return typeName
}
