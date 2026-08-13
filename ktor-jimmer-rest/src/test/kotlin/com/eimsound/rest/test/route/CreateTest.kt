package com.eimsound.rest.test.route

import com.eimsound.rest.test.entity.Book
import com.eimsound.rest.test.infra.TestEnv
import com.eimsound.rest.test.infra.bookRoutes
import com.eimsound.rest.test.infra.jimmerRestTestApp
import com.eimsound.ktor.validator.ApiError
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateTest {

    @BeforeTest
    fun setUp() {
        TestEnv.cleanDatabase()
    }

    @Test
    fun `create saves entity and applies transformer`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }

        val response = client.post("/book") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"learning graphql","edition":1,"price":50}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val created = response.body<Book>()
        assertEquals("LEARNING GRAPHQL", created.name)
        assertEquals(1, created.edition)
        assertEquals(50.toBigDecimal(), created.price)

        val reloaded = TestEnv.sqlClient.findById(Book::class, created.id)
        assertEquals("LEARNING GRAPHQL", reloaded?.name)
    }

    @Test
    fun `create with blank name returns 400`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }

        val response = client.post("/book") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"","edition":1,"price":50}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = response.body<ApiError>()
        assertEquals("BAD_REQUEST", error.code)
        assertTrue(error.errors.any { it.contains("名称不能为空") }, error.errors.toString())
    }

    @Test
    fun `create with price out of range returns 400`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }

        val response = client.post("/book") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Expensive Book","edition":1,"price":150}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
