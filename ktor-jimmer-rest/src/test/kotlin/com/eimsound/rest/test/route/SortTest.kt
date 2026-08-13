package com.eimsound.rest.test.route

import com.eimsound.rest.test.dto.BookPageDto
import com.eimsound.rest.test.infra.TestEnv
import com.eimsound.rest.test.infra.jimmerRestTestApp
import com.eimsound.rest.test.infra.sortRoutes
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.server.testing.testApplication
import io.ktor.http.HttpStatusCode
import java.math.BigDecimal
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SortTest {

    @BeforeTest
    fun setUp() {
        TestEnv.cleanDatabase()
    }

    @Test
    fun `sort by price descending`() = testApplication {
        val client = jimmerRestTestApp { sortRoutes() }
        TestEnv.saveBook(name = "Book A", edition = 1, price = BigDecimal("50"))
        TestEnv.saveBook(name = "Book B", edition = 1, price = BigDecimal("30"))
        TestEnv.saveBook(name = "Book C", edition = 1, price = BigDecimal("80"))

        val page = client.get("/book-sort?sort=price,desc").body<BookPageDto>()

        assertEquals(3, page.rows.size)
        assertEquals(0, BigDecimal("80").compareTo(page.rows[0].price))
        assertEquals(0, BigDecimal("30").compareTo(page.rows[2].price))
    }

    @Test
    fun `sort by price ascending`() = testApplication {
        val client = jimmerRestTestApp { sortRoutes() }
        TestEnv.saveBook(name = "Book A", edition = 1, price = BigDecimal("50"))
        TestEnv.saveBook(name = "Book B", edition = 1, price = BigDecimal("30"))
        TestEnv.saveBook(name = "Book C", edition = 1, price = BigDecimal("80"))

        val page = client.get("/book-sort?sort=price,asc").body<BookPageDto>()

        assertEquals(0, BigDecimal("30").compareTo(page.rows[0].price))
        assertEquals(0, BigDecimal("80").compareTo(page.rows[2].price))
    }

    @Test
    fun `sort supports multiple fields`() = testApplication {
        val client = jimmerRestTestApp { sortRoutes() }
        TestEnv.saveBook(name = "Book A", edition = 1, price = BigDecimal("50"))
        TestEnv.saveBook(name = "Book B", edition = 1, price = BigDecimal("50"))
        TestEnv.saveBook(name = "Book C", edition = 1, price = BigDecimal("30"))

        val page = client.get("/book-sort?sort=price,desc&sort=name,asc").body<BookPageDto>()

        assertEquals(listOf("Book A", "Book B", "Book C"), page.rows.map { it.name })
    }

    @Test
    fun `sort with unknown field returns 400`() = testApplication {
        val client = jimmerRestTestApp { sortRoutes() }
        TestEnv.saveBook(name = "Book A", edition = 1, price = BigDecimal("50"))

        val response = client.get("/book-sort?sort=unknown,asc")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `sort with invalid direction returns 400`() = testApplication {
        val client = jimmerRestTestApp { sortRoutes() }
        TestEnv.saveBook(name = "Book A", edition = 1, price = BigDecimal("50"))

        val response = client.get("/book-sort?sort=price,sideways")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
