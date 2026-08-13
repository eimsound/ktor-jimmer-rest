package com.eimsound.rest.test.route

import com.eimsound.ktor.plugin.*
import com.eimsound.rest.test.infra.TestEnv
import com.eimsound.rest.test.infra.orderRoutes
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson3.jackson
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollisionTest {

    @BeforeTest
    fun setUp() {
        TestEnv.cleanDatabase()
    }

    @Test
    fun `nested parameter colliding with root scalar property fails fast`() = testApplication {
        application {
            install(ContentNegotiation) {
                jackson {
                    addModule(ImmutableModuleV3())
                }
            }
            install(StatusPages) {
                exception<IllegalStateException> { call, cause ->
                    call.respondText(cause.message ?: "", status = HttpStatusCode.InternalServerError)
                }
            }
            install(JimmerRest) {
                jimmerSqlClientFactory {
                    lazy { TestEnv.sqlClient }
                }
            }
            routing { orderRoutes() }
        }
        val store = TestEnv.saveBookStore("MANNING")
        TestEnv.saveOrderItem(code = "O1", store = store)

        val response = client.get("/order-item?store_name=MANNING")

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("store_name"), body)
        assertTrue(body.contains("subParameterSeparator"), body)
    }
}
