package com.eimsound.rest.test.route

import com.eimsound.rest.test.infra.TestEnv
import com.eimsound.rest.test.infra.bookRoutes
import com.eimsound.rest.test.infra.jimmerRestTestApp
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.server.testing.testApplication
import java.math.BigDecimal
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CountExistsTest {

    @BeforeTest
    fun setUp() {
        TestEnv.cleanDatabase()
    }

    @Test
    fun `count returns filtered total`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }
        TestEnv.saveBook(name = "Learning GraphQL", edition = 1, price = BigDecimal("50"))
        TestEnv.saveBook(name = "Learning GraphQL", edition = 2, price = BigDecimal("55"))
        TestEnv.saveBook(name = "Effective TypeScript", edition = 1, price = BigDecimal("73"))

        val count = client.get("/book/count?name__start=Learning").body<Long>()

        assertEquals(2L, count)
    }

    @Test
    fun `count returns total without filter`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }
        repeat(3) {
            TestEnv.saveBook(name = "Book $it", edition = 1, price = BigDecimal(10 + it))
        }

        val count = client.get("/book/count").body<Long>()

        assertEquals(3L, count)
    }

    @Test
    fun `exists returns true for existing id`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }
        val book = TestEnv.saveBook(name = "Existing", edition = 1, price = BigDecimal("50"))

        val exists = client.get("/book/exists/${book.id}").body<Boolean>()

        assertTrue(exists)
    }

    @Test
    fun `exists returns false for missing id`() = testApplication {
        val client = jimmerRestTestApp { bookRoutes() }

        val exists = client.get("/book/exists/999999").body<Boolean>()

        assertFalse(exists)
    }
}
