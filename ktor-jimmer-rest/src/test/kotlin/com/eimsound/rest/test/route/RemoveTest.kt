package com.eimsound.rest.test.route

import com.eimsound.rest.test.entity.Book
import com.eimsound.rest.test.infra.TestEnv
import com.eimsound.rest.test.infra.bookRoutes
import com.eimsound.rest.test.infra.jimmerRestTestApp
import io.ktor.client.request.delete
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.math.BigDecimal
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RemoveTest {

    @BeforeTest
    fun setUp() {
        TestEnv.cleanDatabase()
    }

    @Test
    fun `remove deletes by id`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }
        val book = TestEnv.saveBook(name = "Learning GraphQL", edition = 1, price = BigDecimal("50"))

        val response = client.delete("/book/${book.id}")

        assertEquals(HttpStatusCode.OK, response.status)
        assertNull(TestEnv.sqlClient.findById(Book::class, book.id))
    }

    @Test
    fun `remove missing id is idempotent`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }

        val response = client.delete("/book/999999")

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
