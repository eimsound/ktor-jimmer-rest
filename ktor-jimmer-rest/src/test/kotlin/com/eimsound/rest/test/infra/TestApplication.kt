package com.eimsound.rest.test.infra

import com.eimsound.ktor.plugin.*
import com.eimsound.ktor.provider.*
import com.eimsound.ktor.route.api
import com.eimsound.ktor.validator.exception.ValidationException
import com.eimsound.rest.test.entity.*
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.parsing.ParseException
import io.ktor.serialization.jackson3.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3
import org.babyfish.jimmer.sql.kt.ast.expression.*

fun Application.jimmerRestTestModule(routes: Routing.() -> Unit) {
    install(ContentNegotiation) {
        jackson {
            addModule(ImmutableModuleV3())
        }
    }
    install(StatusPages) {
        exception<ValidationException> { call, cause ->
            call.respond(cause.httpStatusCode, cause.errors)
        }
        exception<ParseException> { call, cause ->
            call.respondText(cause.message ?: "Bad Request", status = HttpStatusCode.BadRequest)
        }
    }
    install(JimmerRest) {
        jimmerSqlClientFactory {
            lazy { TestEnv.sqlClient }
        }
    }
    routing { routes() }
}

fun ApplicationTestBuilder.jimmerRestTestApp(routes: Routing.() -> Unit): HttpClient =
    createClient {
        install(ClientContentNegotiation) {
            jackson {
                addModule(ImmutableModuleV3())
            }
        }
    }.also {
        application { jimmerRestTestModule(routes) }
    }

fun Routing.bookRoutes(path: String = "/book", pagingEnabled: Boolean = true) {
    api<Book>(path) {
        filter {
            where(
                `ilike?`(table::name),
                `between?`(table::price)
            )
            orderBy(table.id.desc())
        }
        fetcher {
            fetch.by {
                allScalarFields()
                store {
                    name()
                    website()
                }
            }
        }
        input {
            validator {
                with(it) {
                    ::name.notBlank { "名称不能为空" }
                    ::price.range(0.toBigDecimal()..100.toBigDecimal()) { range ->
                        "价格必须在${range.start}和${range.endInclusive}之间"
                    }
                }
            }
            transformer {
                it.copy { name = it.name.uppercase() }
            }
        }
        pager { enabled = pagingEnabled }
    }
}

fun Routing.mappingRoutes(path: String = "/book") {
    api<Book>(path) {
        filter {
            where(
                `ilike?`(table::name),
                `ilike?`(table.store::name)
            )
        }
    }
}
