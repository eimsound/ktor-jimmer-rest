package com.eimsound.rest.test.route

import com.eimsound.rest.test.entity.Book
import com.eimsound.rest.test.infra.TestEnv
import com.eimsound.rest.test.infra.bookRoutes
import com.eimsound.rest.test.infra.jimmerRestTestApp
import com.eimsound.ktor.validator.ApiError
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class IdTest {

    @BeforeTest
    fun setUp() {
        TestEnv.cleanDatabase()
    }

    @Test
    fun `get by id returns entity with fetcher projection`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }
        val store = TestEnv.saveBookStore("O'REILLY")
        val book = TestEnv.saveBook(name = "Learning GraphQL", edition = 1, store = store)

        val response = client.get("/book/${book.id}")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<Book>()
        assertEquals(book.id, body.id)
        assertEquals("Learning GraphQL", body.name)
        assertEquals("O'REILLY", body.store?.name)
    }

    @Test
    fun `get by missing id returns 404`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }

        val response: HttpResponse = client.get("/book/999999")

        assertEquals(HttpStatusCode.NotFound, response.status)
        val error = response.body<ApiError>()
        assertEquals(404, error.status)
        assertEquals("NOT_FOUND", error.code)
    }
}
