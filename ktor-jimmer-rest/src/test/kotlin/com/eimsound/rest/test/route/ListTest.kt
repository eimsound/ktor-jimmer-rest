package com.eimsound.rest.test.route

import com.eimsound.rest.test.dto.BookPageDto
import com.eimsound.rest.test.entity.Book
import com.eimsound.rest.test.infra.TestEnv
import com.eimsound.rest.test.infra.bookRoutes
import com.eimsound.rest.test.infra.jimmerRestTestApp
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.math.BigDecimal
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ListTest {

    @BeforeTest
    fun setUp() {
        TestEnv.cleanDatabase()
    }

    @Test
    fun `list filters by ilike start`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }
        TestEnv.saveBook(name = "Learning GraphQL", edition = 1, price = BigDecimal("50"))
        TestEnv.saveBook(name = "Effective TypeScript", edition = 1, price = BigDecimal("73"))

        val response = client.get("/book?name__start=Learning")

        assertEquals(HttpStatusCode.OK, response.status)
        val page = response.body<BookPageDto>()
        assertEquals(1, page.rows.size)
        assertEquals("Learning GraphQL", page.rows[0].name)
    }

    @Test
    fun `list filters by price range`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }
        TestEnv.saveBook(name = "Book A", edition = 1, price = BigDecimal("40"))
        TestEnv.saveBook(name = "Book B", edition = 1, price = BigDecimal("60"))
        TestEnv.saveBook(name = "Book C", edition = 1, price = BigDecimal("90"))

        val response = client.get("/book?price__ge=50&price__le=80")

        assertEquals(HttpStatusCode.OK, response.status)
        val page = response.body<BookPageDto>()
        assertEquals(1, page.rows.size)
        assertEquals("Book B", page.rows[0].name)
    }

    @Test
    fun `list paginates with pageIndex and pageSize`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }
        repeat(5) { index ->
            TestEnv.saveBook(name = "Book $index", edition = 1, price = BigDecimal(10 + index))
        }

        val response = client.get("/book?pageIndex=0&pageSize=2")

        assertEquals(HttpStatusCode.OK, response.status)
        val page = response.body<BookPageDto>()
        assertEquals(2, page.rows.size)
        assertEquals(5, page.totalRowCount)
    }

    @Test
    fun `list returns raw array when paging disabled`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes(pagingEnabled = false) }
        repeat(3) { index ->
            TestEnv.saveBook(name = "Book $index", edition = 1, price = BigDecimal(10 + index))
        }

        val response = client.get("/book")

        assertEquals(HttpStatusCode.OK, response.status)
        val rows = response.body<List<Book>>()
        assertEquals(3, rows.size)
    }

    @Test
    fun `list returns empty page when no rows`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }

        val response = client.get("/book")

        assertEquals(HttpStatusCode.OK, response.status)
        val page = response.body<BookPageDto>()
        assertEquals(0, page.rows.size)
        assertEquals(0, page.totalRowCount)
    }
}
