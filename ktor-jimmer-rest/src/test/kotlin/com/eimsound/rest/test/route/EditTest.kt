package com.eimsound.rest.test.route

import com.eimsound.rest.test.entity.Book
import com.eimsound.rest.test.infra.TestEnv
import com.eimsound.rest.test.infra.bookRoutes
import com.eimsound.rest.test.infra.jimmerRestTestApp
import io.ktor.client.call.body
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.math.BigDecimal
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EditTest {

    @BeforeTest
    fun setUp() {
        TestEnv.cleanDatabase()
    }

    @Test
    fun `edit updates entity`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }
        val book = TestEnv.saveBook(name = "Learning GraphQL", edition = 1, price = BigDecimal("50"))

        val response = client.put("/book") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"id":${book.id},"name":"Learning GraphQL","edition":1,"price":55}"""
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updated = response.body<Book>()
        assertEquals(0, BigDecimal("55").compareTo(updated.price))

        val reloaded = TestEnv.sqlClient.findById(Book::class, book.id)
        assertEquals(0, BigDecimal("55").compareTo(reloaded?.price))
        assertEquals("LEARNING GRAPHQL", reloaded?.name)
    }

    @Test
    fun `edit with invalid price returns 400`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }
        val book = TestEnv.saveBook(name = "Learning GraphQL", edition = 1, price = BigDecimal("50"))

        val response = client.put("/book") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"id":${book.id},"name":"Learning GraphQL","edition":1,"price":150}"""
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
