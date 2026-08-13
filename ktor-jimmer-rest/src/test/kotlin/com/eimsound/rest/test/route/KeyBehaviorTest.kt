package com.eimsound.rest.test.route

import com.eimsound.rest.test.entity.Book
import com.eimsound.rest.test.infra.TestEnv
import com.eimsound.rest.test.infra.bookRoutes
import com.eimsound.rest.test.infra.jimmerRestTestApp
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.math.BigDecimal
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KeyBehaviorTest {

    @BeforeTest
    fun setUp() {
        TestEnv.cleanDatabase()
    }

    private fun seedTwoBooks(): Pair<Book, Book> {
        val first = TestEnv.saveBook(name = "Book A", edition = 1, price = BigDecimal("10"))
        val second = TestEnv.saveBook(name = "Book B", edition = 1, price = BigDecimal("20"))
        return first to second
    }

    @Test
    fun `fixed key overrides path variable for id route`() = testApplication {
        val (first, second) = seedTwoBooks()
        val client = jimmerRestTestApp { bookRoutes(key = second.id) }

        val response = client.get("/book/${first.id}")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<Book>()
        assertEquals(second.id, body.id)
    }

    @Test
    fun `fixed key applies to remove route`() = testApplication {
        val (first, second) = seedTwoBooks()
        val client = jimmerRestTestApp { bookRoutes(key = second.id) }

        val response = client.delete("/book/${first.id}")

        assertEquals(HttpStatusCode.OK, response.status)
        assertNull(TestEnv.sqlClient.findById(Book::class, second.id))
        assertNotNull(TestEnv.sqlClient.findById(Book::class, first.id))
    }
}
