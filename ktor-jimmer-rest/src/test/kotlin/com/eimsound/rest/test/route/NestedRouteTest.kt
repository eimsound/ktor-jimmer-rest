package com.eimsound.rest.test.route

import com.eimsound.rest.test.entity.Book
import com.eimsound.rest.test.infra.TestEnv
import com.eimsound.rest.test.infra.bookRoutes
import com.eimsound.rest.test.infra.jimmerRestTestApp
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.route
import io.ktor.server.testing.testApplication
import java.math.BigDecimal
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NestedRouteTest {

    @BeforeTest
    fun setUp() {
        TestEnv.cleanDatabase()
    }

    @Test
    fun `id route resolves its own path variable under nested route`() = testApplication {
        val client = jimmerRestTestApp { route("/{tenant}") { bookRoutes() } }
        val book = TestEnv.saveBook(name = "Learning GraphQL", edition = 1, price = BigDecimal("50"))

        val response = client.get("/t1/book/${book.id}")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<Book>()
        assertEquals(book.id, body.id)
    }

    @Test
    fun `remove route resolves its own path variable under nested route`() = testApplication {
        val client = jimmerRestTestApp { route("/{tenant}") { bookRoutes() } }
        val book = TestEnv.saveBook(name = "Learning GraphQL", edition = 1, price = BigDecimal("50"))

        val response = client.delete("/t1/book/${book.id}")

        assertEquals(HttpStatusCode.OK, response.status)
        assertNull(TestEnv.sqlClient.findById(Book::class, book.id))
    }
}
