package com.eimsound.rest.test.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.ManyToMany

@Entity
interface Author : BaseEntity {
    @Key
    val firstName: String

    @Key
    val lastName: String

    @ManyToMany(mappedBy = "authors")
    val books: List<Book>

    val gender: Gender
}
