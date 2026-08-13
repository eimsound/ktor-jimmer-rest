package com.eimsound.rest.test.route

import com.eimsound.rest.test.infra.TestEnv
import com.eimsound.rest.test.infra.bookRoutes
import com.eimsound.rest.test.infra.jimmerRestTestApp
import com.eimsound.ktor.validator.ApiError
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorTest {

    @BeforeTest
    fun setUp() {
        TestEnv.cleanDatabase()
    }

    @Test
    fun `unparsable query parameter returns 400`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }

        val response = client.get("/book?price__ge=abc")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = response.body<ApiError>()
        assertEquals("BAD_REQUEST", error.code)
    }
}
