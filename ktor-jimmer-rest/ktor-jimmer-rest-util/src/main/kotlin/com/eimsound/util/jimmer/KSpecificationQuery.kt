package com.eimsound.util.jimmer

import org.babyfish.jimmer.sql.ast.query.Order
import org.babyfish.jimmer.sql.kt.ast.expression.KExpression
import org.babyfish.jimmer.sql.kt.ast.expression.KNonNullExpression
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable

interface KSpecificationQuery<T : Any> {
    val table: KNonNullTable<T>

    fun orderBy(vararg orders: Order?)

    fun orderBy(vararg expressions: KExpression<T>?)

    fun orderBy(orders: List<Order?>)

    fun groupBy(vararg expressions: KExpression<T>)

    fun having(vararg predicates: KNonNullExpression<Boolean>?)
}
