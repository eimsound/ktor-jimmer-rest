package com.eimsound.rest.test.route

import com.eimsound.rest.test.dto.BookPageDto
import com.eimsound.rest.test.infra.TestEnv
import com.eimsound.rest.test.infra.jimmerRestTestApp
import com.eimsound.rest.test.infra.mappingRoutes
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.math.BigDecimal
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ParameterMappingTest {

    @BeforeTest
    fun setUp() {
        TestEnv.cleanDatabase()
    }

    @Test
    fun `root property maps to plain parameter`() = testApplication {
        val client = jimmerRestTestApp { mappingRoutes() }
        TestEnv.saveBook(name = "Learning GraphQL", edition = 1, price = BigDecimal("50"))
        TestEnv.saveBook(name = "Effective TypeScript", edition = 1, price = BigDecimal("73"))

        val response = client.get("/book?name=Learning")

        assertEquals(HttpStatusCode.OK, response.status)
        val page = response.body<BookPageDto>()
        assertEquals(1, page.rows.size)
        assertEquals("Learning GraphQL", page.rows[0].name)
    }

    @Test
    fun `ext suffix maps to like mode`() = testApplication {
        val client = jimmerRestTestApp { mappingRoutes() }
        TestEnv.saveBook(name = "Learning GraphQL", edition = 1, price = BigDecimal("50"))
        TestEnv.saveBook(name = "Learning GraphQL", edition = 2, price = BigDecimal("55"))
        TestEnv.saveBook(name = "Learning TypeScript", edition = 1, price = BigDecimal("60"))

        val response = client.get("/book?name__exact=Learning GraphQL")

        assertEquals(HttpStatusCode.OK, response.status)
        val page = response.body<BookPageDto>()
        assertEquals(2, page.rows.size)
    }

    @Test
    fun `nested table property maps to sub parameter name`() = testApplication {
        val client = jimmerRestTestApp { mappingRoutes() }
        val manning = TestEnv.saveBookStore("MANNING")
        TestEnv.saveBook(name = "GraphQL in Action", edition = 1, price = BigDecimal("80"), store = manning)
        TestEnv.saveBook(name = "Learning GraphQL", edition = 1, price = BigDecimal("50"))

        val response = client.get("/book?store_name=MANNING")

        assertEquals(HttpStatusCode.OK, response.status)
        val page = response.body<BookPageDto>()
        assertEquals(1, page.rows.size)
        assertEquals("GraphQL in Action", page.rows[0].name)
    }

    @Test
    fun `missing parameters match all rows`() = testApplication {
        val client = jimmerRestTestApp { mappingRoutes() }
        TestEnv.saveBook(name = "Book A", edition = 1, price = BigDecimal("40"))
        TestEnv.saveBook(name = "Book B", edition = 1, price = BigDecimal("60"))

        val response = client.get("/book")

        assertEquals(HttpStatusCode.OK, response.status)
        val page = response.body<BookPageDto>()
        assertEquals(2, page.rows.size)
    }
}
