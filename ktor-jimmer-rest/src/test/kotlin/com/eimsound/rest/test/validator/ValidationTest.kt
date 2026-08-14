package com.eimsound.rest.test.validator

import com.eimsound.ktor.provider.Validators
import com.eimsound.ktor.provider.validate
import com.eimsound.rest.test.entity.Book
import org.babyfish.jimmer.Input
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ValidationTest {

    private class BookInput(val name: String) : Input<Book> {
        override fun toEntity(): Book = Book {
            this.name = name
            edition = 1
            price = BigDecimal("10")
        }
    }

    @Test
    fun `entity validator fails fast when input is InputType`() {
        // 模拟配置错位：validator 是 Entity 类型，但入参是 Input 类型
        val entityValidator: Validators<Book>? = Validators.Entity<Book> {
            it::name.notBlank { "名称不能为空" }
        }
        val mismatch = assertFailsWith<IllegalStateException> {
            entityValidator.validate(BookInput("Valid"))
        }
        assertTrue(mismatch.message!!.contains("不匹配"))
    }
}
