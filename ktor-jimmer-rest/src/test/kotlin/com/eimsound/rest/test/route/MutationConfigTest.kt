package com.eimsound.rest.test.route

import com.eimsound.rest.test.entity.*
import com.eimsound.rest.test.infra.TestEnv
import com.eimsound.rest.test.infra.bookRoutes
import com.eimsound.rest.test.infra.jimmerRestTestApp
import com.eimsound.rest.test.infra.keyResolverRoutes
import com.eimsound.rest.test.infra.patchRoutes
import com.eimsound.rest.test.infra.patchIndependentRoutes
import com.eimsound.rest.test.infra.projectionRoutes
import com.eimsound.rest.test.infra.upsertRoutes
import com.eimsound.ktor.route.api
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import java.math.BigDecimal
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MutationConfigTest {

    @BeforeTest
    fun setUp() {
        TestEnv.cleanDatabase()
    }

    @Test
    fun `api block runs once at registration`() = testApplication {
        var executions = 0
        val client = jimmerRestTestApp {
            api<Book>("/book-count") {
                executions++
            }
        }

        repeat(3) {
            client.get("/book-count")
        }

        assertEquals(1, executions)
    }

    @Test
    fun `create with merge save mode upserts on same business key`() = testApplication {
        val client = jimmerRestTestApp { upsertRoutes() }

        val first = client.post("/book-upsert") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"UPSERT","edition":1,"price":50}""")
        }
        val second = client.post("/book-upsert") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"UPSERT","edition":1,"price":60}""")
        }

        assertEquals(HttpStatusCode.OK, first.status)
        assertEquals(HttpStatusCode.OK, second.status)
        val books = TestEnv.sqlClient.createQuery(Book::class) {
            where(table.name eq "UPSERT")
            select(table)
        }.execute()
        assertEquals(1, books.size)
        assertEquals(0, BigDecimal("60").compareTo(books[0].price))
    }

    @Test
    fun `create response uses projection fetcher`() = testApplication {
        val client = jimmerRestTestApp { projectionRoutes() }

        val response = client.post("/book-projection") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Projection","edition":1,"price":50}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val text = response.bodyAsText()
        assertTrue(text.contains("\"name\""), text)
        assertFalse(text.contains("\"price\""), text)
    }

    @Test
    fun `edit response uses projection fetcher`() = testApplication {
        val client = jimmerRestTestApp { projectionRoutes() }
        val book = TestEnv.saveBook(name = "Projection", edition = 1, price = BigDecimal("50"))

        val response = client.put("/book-projection") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":${book.id},"name":"Projection","edition":1,"price":55}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val text = response.bodyAsText()
        assertTrue(text.contains("\"name\""), text)
        assertFalse(text.contains("\"price\""), text)
    }

    @Test
    fun `patch updates only provided fields`() = testApplication {
        val client = jimmerRestTestApp { patchRoutes() }
        val book = TestEnv.saveBook(name = "Keep Name", edition = 1, price = BigDecimal("50"))

        val response = client.patch("/book-patch") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":${book.id},"price":80}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val reloaded = TestEnv.sqlClient.findById(Book::class, book.id)
        assertEquals("Keep Name", reloaded?.name)
        assertEquals(0, BigDecimal("80").compareTo(reloaded?.price))
    }

    @Test
    fun `patch route not registered without patch block`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }
        val book = TestEnv.saveBook(name = "No Patch", edition = 1, price = BigDecimal("50"))

        val response = client.patch("/book") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":${book.id},"price":80}""")
        }

        assertEquals(HttpStatusCode.MethodNotAllowed, response.status)
    }

    @Test
    fun `key resolver provides id from request context for id route`() = testApplication {
        val client = jimmerRestTestApp { keyResolverRoutes() }
        val first = TestEnv.saveBook(name = "Book A", edition = 1, price = BigDecimal("10"))
        val second = TestEnv.saveBook(name = "Book B", edition = 1, price = BigDecimal("20"))

        val response = client.get("/book-key/${first.id}?keyId=${second.id}")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<Book>()
        assertEquals(second.id, body.id)
    }

    @Test
    fun `patch has independent config from edit`() = testApplication {
        val client = jimmerRestTestApp { patchIndependentRoutes() }
        val book = TestEnv.saveBook(name = "Independent", edition = 1, price = BigDecimal("50"))

        val patchResponse = client.patch("/book-patch-independent") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":${book.id},"price":60}""")
        }
        val putResponse = client.put("/book-patch-independent") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":${book.id},"name":"Independent","edition":1,"price":70}""")
        }

        assertEquals(HttpStatusCode.OK, patchResponse.status)
        val patchText = patchResponse.bodyAsText()
        assertTrue(patchText.contains("\"name\""), patchText)
        assertFalse(patchText.contains("\"price\""), patchText)

        assertEquals(HttpStatusCode.OK, putResponse.status)
        val putText = putResponse.bodyAsText()
        assertTrue(putText.contains("\"price\""), putText)
    }
}
