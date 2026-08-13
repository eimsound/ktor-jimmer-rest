package com.eimsound.rest.test.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.ManyToOne

@Entity
interface OrderItem : BaseEntity {
    @Key
    val code: String

    @ManyToOne
    val store: BookStore?

    // 蛇形属性名 → 查询参数名 store_name，与嵌套路径 store.name 冲突（用于 D3 测试）
    val store_name: String?
}
