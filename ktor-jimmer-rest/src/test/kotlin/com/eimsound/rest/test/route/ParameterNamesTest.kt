package com.eimsound.rest.test.route

import com.eimsound.rest.test.entity.*
import com.eimsound.rest.test.infra.TestEnv
import com.eimsound.util.ktor.ParameterNames
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ParameterNamesTest {

    private fun bookTable(): KNonNullTable<Book> {
        var table: KNonNullTable<Book>? = null
        TestEnv.sqlClient.createQuery(Book::class) {
            table = this.table
            select(table)
        }
        return checkNotNull(table)
    }

    @Test
    fun `resolve maps root property to plain parameter name`() {
        assertEquals("name", ParameterNames.resolve(bookTable()::name))
    }

    @Test
    fun `resolve maps nested table property to sub parameter name`() {
        assertEquals("store_name", ParameterNames.resolve(bookTable().store::name))
    }

    @Test
    fun `resolve is consistent across calls`() {
        val table = bookTable()
        assertEquals(ParameterNames.resolve(table::name), ParameterNames.resolve(table::name))
        assertEquals(
            ParameterNames.resolve(table.store::name),
            ParameterNames.resolve(table.store::name)
        )
    }

    @Test
    fun `unbound property reference fails with clear message`() {
        val e = assertFailsWith<IllegalArgumentException> {
            ParameterNames.resolve(Book::name)
        }
        assertTrue(e.message!!.contains("绑定的属性引用"), e.message)
    }
}
