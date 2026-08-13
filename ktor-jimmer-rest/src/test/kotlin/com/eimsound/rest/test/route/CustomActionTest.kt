package com.eimsound.rest.test.route

import com.eimsound.rest.test.infra.TestEnv
import com.eimsound.rest.test.infra.customActionRoutes
import com.eimsound.rest.test.infra.jimmerRestTestApp
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.math.BigDecimal
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomActionTest {

    @BeforeTest
    fun setUp() {
        TestEnv.cleanDatabase()
    }

    @Test
    fun `custom action registers stats route under api path`() = testApplication {
        val client = jimmerRestTestApp { customActionRoutes() }
        TestEnv.saveBook(name = "Book A", edition = 1, price = BigDecimal("10"))
        TestEnv.saveBook(name = "Book B", edition = 1, price = BigDecimal("20"))

        val response = client.get("/book-custom/stats")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"count\":2"), body)
    }

    @Test
    fun `custom action supports path parameters`() = testApplication {
        val client = jimmerRestTestApp { customActionRoutes() }

        val response = client.post("/book-custom/7/publish")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"id\":7"), body)
        assertTrue(body.contains("\"published\":true"), body)
    }
}
