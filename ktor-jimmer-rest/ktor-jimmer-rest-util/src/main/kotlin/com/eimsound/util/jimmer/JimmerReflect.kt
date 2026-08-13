package com.eimsound.util.jimmer

import com.eimsound.util.reflect.getPropertyOwner
import org.babyfish.jimmer.sql.Id
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

fun <T> getPropertyFullName(property: KProperty<T>, bound: KClass<*>): String {
    val propertyOwner = getPropertyOwner(property)
    if (propertyOwner == bound) {
        return property.name
    }
    val simpleName = propertyOwner.simpleName
    return "$simpleName.${property.name}"
}

/**
 * 返回实体唯一的 `@Id` 属性类型。
 *
 * Jimmer 不支持复合主键（KSP 编译期拒绝多个 `@Id`），此处做防御性校验；
 * 如需复合业务键，请使用 `@Key`（save/upsert 已支持）。
 */
inline fun <reified T : Any> entityIdType(): KClass<*> {
    val ids = T::class.memberProperties.filter {
        it.annotations.any { annotation -> annotation.annotationClass == Id::class }
    }
    require(ids.size == 1) {
        "实体 ${T::class.simpleName} 必须且只能有一个 @Id 属性（Jimmer 不支持复合主键），实际 ${ids.size} 个"
    }
    return ids.single().returnType.classifier as KClass<*>
}
