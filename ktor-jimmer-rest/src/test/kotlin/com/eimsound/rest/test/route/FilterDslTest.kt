package com.eimsound.rest.test.route

import com.eimsound.rest.test.dto.BookPageDto
import com.eimsound.rest.test.dto.OrderItemPageDto
import com.eimsound.rest.test.infra.TestEnv
import com.eimsound.rest.test.infra.comparisonRoutes
import com.eimsound.rest.test.infra.eqRoutes
import com.eimsound.rest.test.infra.inRoutes
import com.eimsound.rest.test.infra.inclusiveRoutes
import com.eimsound.rest.test.infra.isNullRoutes
import com.eimsound.rest.test.infra.jimmerRestTestApp
import com.eimsound.rest.test.infra.notEqRoutes
import com.eimsound.rest.test.infra.notInRoutes
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.server.testing.testApplication
import java.math.BigDecimal
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FilterDslTest {

    @BeforeTest
    fun setUp() {
        TestEnv.cleanDatabase()
    }

    // ---------- B1: eq? ----------

    @Test
    fun `eq matches plain parameter`() = testApplication {
        val client = jimmerRestTestApp { eqRoutes() }
        TestEnv.saveBook(name = "Learning GraphQL", edition = 1, price = BigDecimal("50"))
        TestEnv.saveBook(name = "Effective TypeScript", edition = 1, price = BigDecimal("73"))

        val page = client.get("/book-eq?name=Learning%20GraphQL").body<BookPageDto>()

        assertEquals(1, page.rows.size)
        assertEquals("Learning GraphQL", page.rows[0].name)
    }

    @Test
    fun `eq matches exact ext`() = testApplication {
        val client = jimmerRestTestApp { eqRoutes() }
        TestEnv.saveBook(name = "Learning GraphQL", edition = 1, price = BigDecimal("50"))
        TestEnv.saveBook(name = "Effective TypeScript", edition = 1, price = BigDecimal("73"))

        val page = client.get("/book-eq?name__exact=Learning%20GraphQL").body<BookPageDto>()

        assertEquals(1, page.rows.size)
        assertEquals("Learning GraphQL", page.rows[0].name)
    }

    @Test
    fun `eq prefers plain parameter regardless of order`() = testApplication {
        val client = jimmerRestTestApp { eqRoutes() }
        TestEnv.saveBook(name = "Learning GraphQL", edition = 1, price = BigDecimal("50"))
        TestEnv.saveBook(name = "Effective TypeScript", edition = 1, price = BigDecimal("73"))

        val page1 = client.get("/book-eq?name=Effective%20TypeScript&name__exact=Learning%20GraphQL")
            .body<BookPageDto>()
        val page2 = client.get("/book-eq?name__exact=Learning%20GraphQL&name=Effective%20TypeScript")
            .body<BookPageDto>()

        assertEquals(1, page1.rows.size)
        assertEquals("Effective TypeScript", page1.rows[0].name)
        assertEquals(page1.rows, page2.rows)
    }

    @Test
    fun `eq ignores unrelated ext`() = testApplication {
        val client = jimmerRestTestApp { eqRoutes() }
        TestEnv.saveBook(name = "Learning GraphQL", edition = 1, price = BigDecimal("50"))
        TestEnv.saveBook(name = "Effective TypeScript", edition = 1, price = BigDecimal("73"))

        val page = client.get("/book-eq?name__start=Learning").body<BookPageDto>()

        assertEquals(2, page.rows.size)
    }

    // ---------- B2: in? / notIn? ----------

    @Test
    fun `in supports comma separated values`() = testApplication {
        val client = jimmerRestTestApp { inRoutes() }
        val first = TestEnv.saveBook(name = "Book A", edition = 1, price = BigDecimal("10"))
        TestEnv.saveBook(name = "Book B", edition = 1, price = BigDecimal("20"))
        val third = TestEnv.saveBook(name = "Book C", edition = 1, price = BigDecimal("30"))

        val page = client.get("/book-in?id=${first.id},${third.id}").body<BookPageDto>()

        assertEquals(2, page.rows.size)
    }

    @Test
    fun `in supports repeated parameters`() = testApplication {
        val client = jimmerRestTestApp { inRoutes() }
        val first = TestEnv.saveBook(name = "Book A", edition = 1, price = BigDecimal("10"))
        TestEnv.saveBook(name = "Book B", edition = 1, price = BigDecimal("20"))
        val third = TestEnv.saveBook(name = "Book C", edition = 1, price = BigDecimal("30"))

        val page = client.get("/book-in?id=${first.id}&id=${third.id}").body<BookPageDto>()

        assertEquals(2, page.rows.size)
    }

    @Test
    fun `in with no values matches all`() = testApplication {
        val client = jimmerRestTestApp { inRoutes() }
        repeat(3) { TestEnv.saveBook(name = "Book $it", edition = 1, price = BigDecimal(10 + it)) }

        val page = client.get("/book-in").body<BookPageDto>()

        assertEquals(3, page.rows.size)
    }

    @Test
    fun `notIn excludes matching ids`() = testApplication {
        val client = jimmerRestTestApp { notInRoutes() }
        val first = TestEnv.saveBook(name = "Book A", edition = 1, price = BigDecimal("10"))
        TestEnv.saveBook(name = "Book B", edition = 1, price = BigDecimal("20"))
        TestEnv.saveBook(name = "Book C", edition = 1, price = BigDecimal("30"))

        val page = client.get("/book-not-in?id=${first.id}").body<BookPageDto>()

        assertEquals(2, page.rows.size)
    }

    // ---------- B2: single-side comparisons ----------

    @Test
    fun `lt and gt narrow price range`() = testApplication {
        val client = jimmerRestTestApp { comparisonRoutes() }
        TestEnv.saveBook(name = "Book A", edition = 1, price = BigDecimal("40"))
        TestEnv.saveBook(name = "Book B", edition = 1, price = BigDecimal("60"))
        TestEnv.saveBook(name = "Book C", edition = 1, price = BigDecimal("90"))

        val page = client.get("/book-comparison?price__lt=80&price__gt=40").body<BookPageDto>()

        assertEquals(1, page.rows.size)
        assertEquals("Book B", page.rows[0].name)
    }

    @Test
    fun `le and ge include boundaries`() = testApplication {
        val client = jimmerRestTestApp { inclusiveRoutes() }
        TestEnv.saveBook(name = "Book A", edition = 1, price = BigDecimal("40"))
        TestEnv.saveBook(name = "Book B", edition = 1, price = BigDecimal("60"))
        TestEnv.saveBook(name = "Book C", edition = 1, price = BigDecimal("90"))

        val page = client.get("/book-inclusive?price__le=60&price__ge=40").body<BookPageDto>()

        assertEquals(2, page.rows.size)
    }

    @Test
    fun `notEq excludes matching name`() = testApplication {
        val client = jimmerRestTestApp { notEqRoutes() }
        TestEnv.saveBook(name = "Book A", edition = 1, price = BigDecimal("10"))
        TestEnv.saveBook(name = "Book B", edition = 1, price = BigDecimal("20"))

        val page = client.get("/book-not-eq?name=Book%20A").body<BookPageDto>()

        assertEquals(1, page.rows.size)
        assertEquals("Book B", page.rows[0].name)
    }

    // ---------- B2: isNull ----------

    @Test
    fun `isNull matches rows with null scalar`() = testApplication {
        val client = jimmerRestTestApp { isNullRoutes() }
        val store = TestEnv.saveBookStore("MANNING")
        TestEnv.saveOrderItem(code = "O1", store = store)
        TestEnv.saveOrderItem(code = "O2", store = store, storeName = "MANNING")

        val page = client.get("/order-null").body<OrderItemPageDto>()

        assertEquals(1, page.rows.size)
        assertEquals("O1", page.rows[0].code)
    }
}
