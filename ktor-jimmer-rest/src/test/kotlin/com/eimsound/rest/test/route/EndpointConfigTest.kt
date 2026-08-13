package com.eimsound.rest.test.route

import com.eimsound.ktor.plugin.*
import com.eimsound.ktor.provider.*
import com.eimsound.ktor.route.*
import com.eimsound.rest.test.dto.BookPageDto
import com.eimsound.rest.test.entity.*
import com.eimsound.rest.test.infra.TestEnv
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson3.jackson
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3
import java.math.BigDecimal
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EndpointConfigTest {

    @BeforeTest
    fun setUp() {
        TestEnv.cleanDatabase()
    }

    @Test
    fun `endpoint config customizes batch path and sort parameter`() = testApplication {
        val client = createClient {
            install(ContentNegotiation) {
                jackson {
                    addModule(ImmutableModuleV3())
                }
            }
        }
        application {
            install(ServerContentNegotiation) {
                jackson {
                    addModule(ImmutableModuleV3())
                }
            }
            install(JimmerRest) {
                jimmerSqlClientFactory {
                    lazy { TestEnv.sqlClient }
                }
                endpoint {
                    batchPath = "bulk"
                    sortParameterName = "orderBy"
                }
            }
            routing {
                api<Book>("/book-endpoint-config") {
                    filter {
                        sort()
                    }
                    batch { }
                }
            }
        }

        val create = client.post("/book-endpoint-config/bulk") {
            contentType(ContentType.Application.Json)
            setBody("""[{"name":"Bulk A","edition":1,"price":50}]""")
        }
        assertEquals(HttpStatusCode.OK, create.status)

        TestEnv.saveBook(name = "Book B", edition = 1, price = BigDecimal("30"))
        TestEnv.saveBook(name = "Book C", edition = 1, price = BigDecimal("80"))
        val page = client.get("/book-endpoint-config?orderBy=price,desc").body<BookPageDto>()
        assertEquals(3, page.rows.size)
        assertEquals(0, BigDecimal("80").compareTo(page.rows[0].price))
    }
}
