package com.eimsound.rest.test.route

import com.eimsound.rest.test.entity.*
import com.eimsound.rest.test.infra.TestEnv
import com.eimsound.rest.test.infra.batchRoutes
import com.eimsound.rest.test.infra.bookRoutes
import com.eimsound.rest.test.infra.jimmerRestTestApp
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.babyfish.jimmer.sql.kt.ast.expression.*
import java.math.BigDecimal
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BatchTest {

    @BeforeTest
    fun setUp() {
        TestEnv.cleanDatabase()
    }

    @Test
    fun `batch create inserts all entities and applies transformer`() = testApplication {
        val client = jimmerRestTestApp { batchRoutes() }

        val response = client.post("/book-batch/batch") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                [
                  {"name":"Batch A","edition":1,"price":10},
                  {"name":"Batch B","edition":1,"price":20}
                ]
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val books = TestEnv.sqlClient.createQuery(Book::class) {
            orderBy(table.name.asc())
            select(table)
        }.execute()
        assertEquals(2, books.size)
        assertEquals("BATCH A", books[0].name)
        assertEquals("BATCH B", books[1].name)
    }

    @Test
    fun `batch create fails whole batch on invalid item`() = testApplication {
        val client = jimmerRestTestApp { batchRoutes() }

        val response = client.post("/book-batch/batch") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                [
                  {"name":"Valid","edition":1,"price":10},
                  {"name":"","edition":1,"price":20}
                ]
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val count = TestEnv.sqlClient.createQuery(Book::class) {
            select(table)
        }.execute().size
        assertEquals(0, count)
    }

    @Test
    fun `batch update updates prices`() = testApplication {
        val client = jimmerRestTestApp { batchRoutes() }
        val first = TestEnv.saveBook(name = "Batch A", edition = 1, price = BigDecimal("10"))
        val second = TestEnv.saveBook(name = "Batch B", edition = 1, price = BigDecimal("20"))

        val response = client.put("/book-batch/batch") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                [
                  {"id":${first.id},"name":"Batch A","edition":1,"price":11},
                  {"id":${second.id},"name":"Batch B","edition":1,"price":22}
                ]
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(0, BigDecimal("11").compareTo(TestEnv.sqlClient.findById(Book::class, first.id)?.price))
        assertEquals(0, BigDecimal("22").compareTo(TestEnv.sqlClient.findById(Book::class, second.id)?.price))
    }

    @Test
    fun `batch delete removes matching ids`() = testApplication {
        val client = jimmerRestTestApp { batchRoutes() }
        val first = TestEnv.saveBook(name = "Batch A", edition = 1, price = BigDecimal("10"))
        val second = TestEnv.saveBook(name = "Batch B", edition = 1, price = BigDecimal("20"))
        val third = TestEnv.saveBook(name = "Batch C", edition = 1, price = BigDecimal("30"))

        val response = client.delete("/book-batch/batch?ids=${first.id},${second.id}")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(null, TestEnv.sqlClient.findById(Book::class, first.id))
        assertEquals(null, TestEnv.sqlClient.findById(Book::class, second.id))
        assertEquals(third.id, TestEnv.sqlClient.findById(Book::class, third.id)?.id)
    }

    @Test
    fun `batch routes not registered without batch block`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }

        val response = client.post("/book/batch") {
            contentType(ContentType.Application.Json)
            setBody("""[{"name":"X","edition":1,"price":10}]""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `batch create with upsert mode updates on same business key`() = testApplication {
        val client = jimmerRestTestApp { batchRoutes(upsert = true) }
        client.post("/book-batch/batch") {
            contentType(ContentType.Application.Json)
            setBody("""[{"name":"UPSERT","edition":1,"price":50}]""")
        }

        val response = client.post("/book-batch/batch") {
            contentType(ContentType.Application.Json)
            setBody("""[{"name":"UPSERT","edition":1,"price":60}]""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val books = TestEnv.sqlClient.createQuery(Book::class) {
            where(table.name eq "UPSERT")
            select(table)
        }.execute()
        assertEquals(1, books.size)
        assertEquals(0, BigDecimal("60").compareTo(books[0].price))
    }

    @Test
    fun `batch config customizes path and ids parameter`() = testApplication {
        val client = jimmerRestTestApp {
            batchRoutes(path = "/book-bulk", batchPath = "bulk", deleteIdsParameterName = "bookIds")
        }
        val first = TestEnv.saveBook(name = "Bulk A", edition = 1, price = BigDecimal("10"))
        val second = TestEnv.saveBook(name = "Bulk B", edition = 1, price = BigDecimal("20"))

        val create = client.post("/book-bulk/bulk") {
            contentType(ContentType.Application.Json)
            setBody("""[{"name":"Bulk C","edition":1,"price":30}]""")
        }
        val del = client.delete("/book-bulk/bulk?bookIds=${first.id},${second.id}")

        assertEquals(HttpStatusCode.OK, create.status)
        assertEquals(HttpStatusCode.OK, del.status)
        assertEquals(null, TestEnv.sqlClient.findById(Book::class, first.id))
        assertEquals(null, TestEnv.sqlClient.findById(Book::class, second.id))
    }
}
