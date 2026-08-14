package com.eimsound.rest.test.infra

import com.eimsound.ktor.plugin.*
import com.eimsound.ktor.provider.*
import com.eimsound.ktor.route.*
import com.eimsound.jimmer.sqlClient
import com.eimsound.rest.test.entity.*
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.serialization.jackson3.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import com.eimsound.ktor.validator.jimmerRestErrors
import org.babyfish.jimmer.jackson.v3.ImmutableModuleV3
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.ast.expression.*

fun Application.jimmerRestTestModule(routes: Routing.() -> Unit) {
    install(ContentNegotiation) {
        jackson {
            addModule(ImmutableModuleV3())
        }
    }
    install(StatusPages) {
        jimmerRestErrors()
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

fun Route.bookRoutes(path: String = "/book", pagingEnabled: Boolean = true, key: Any? = null) {
    api<Book>(path) {
        if (key != null) {
            this.key = key
        }
        filter {
            where(
                `ilike?`(table.name),
                `between?`(table.price)
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

fun Route.expressionRoutes(path: String = "/book-expr") {
    api<Book>(path) {
        filter {
            where(
                `ilike?`(table.name),
                `ilike?`(table.store.name)
            )
            orderBy(table.id.desc())
        }
    }
}

fun Route.mappingRoutes(path: String = "/book") {
    api<Book>(path) {
        filter {
            where(
                `ilike?`(table::name),
                `ilike?`(table.store::name)
            )
        }
    }
}

fun Route.authorRoutes(path: String = "/book-by-author") {
    api<Book>(path) {
        filter {
            where(Book::authors) {
                `ilike?`(table::firstName)
            }
            orderBy(table.id.desc())
        }
    }
}

fun Route.storeRoutes(path: String = "/book-by-store") {
    api<Book>(path) {
        filter {
            where(table.store) {
                `ilike?`(table::name)
            }
            orderBy(table.id.desc())
        }
    }
}

fun Route.nestedStoreRoutes(path: String = "/order-by-store-book") {
    api<OrderItem>(path) {
        filter {
            where(table.store) {
                assoc(BookStore::books) {
                    `ilike?`(table.name)
                }
            }
            orderBy(table.id.desc())
        }
    }
}

fun Route.orderRoutes(path: String = "/order-item") {
    api<OrderItem>(path) {
        filter {
            where(`ilike?`(table.store::name))
        }
    }
}

fun Route.eqRoutes(path: String = "/book-eq") {
    api<Book>(path) {
        filter {
            where(`eq?`(table.name))
        }
    }
}

fun Route.inRoutes(path: String = "/book-in") {
    api<Book>(path) {
        filter {
            where(`in?`(table.id))
        }
    }
}

fun Route.notInRoutes(path: String = "/book-not-in") {
    api<Book>(path) {
        filter {
            where(`notIn?`(table.id))
        }
    }
}

fun Route.comparisonRoutes(path: String = "/book-comparison") {
    api<Book>(path) {
        filter {
            where(
                `lt?`(table.price),
                `gt?`(table.price)
            )
        }
    }
}

fun Route.inclusiveRoutes(path: String = "/book-inclusive") {
    api<Book>(path) {
        filter {
            where(
                `le?`(table.price),
                `ge?`(table.price)
            )
        }
    }
}

fun Route.notEqRoutes(path: String = "/book-not-eq") {
    api<Book>(path) {
        filter {
            where(`notEq?`(table.name))
        }
    }
}

fun Route.isNullRoutes(path: String = "/order-null") {
    api<OrderItem>(path) {
        filter {
            where(isNull(table.store_name))
        }
    }
}

fun Route.sortRoutes(path: String = "/book-sort") {
    api<Book>(path) {
        filter {
            sort()
        }
    }
}

fun Route.upsertRoutes(path: String = "/book-upsert") {
    api<Book>(path) {
        create {
            saveMode = SaveMode.UPSERT
            associatedSaveMode = AssociatedSaveMode.MERGE
        }
    }
}

fun Route.projectionRoutes(path: String = "/book-projection") {
    api<Book>(path) {
        create {
            fetcher {
                fetch.by {
                    name()
                }
            }
        }
        edit {
            fetcher {
                fetch.by {
                    name()
                }
            }
        }
    }
}

fun Route.patchRoutes(path: String = "/book-patch") {
    api<Book>(path) {
        patch { }
    }
}

fun Route.patchIndependentRoutes(path: String = "/book-patch-independent") {
    api<Book>(path) {
        patch {
            fetcher {
                fetch.by {
                    name()
                }
            }
        }
    }
}

fun Route.keyResolverRoutes(path: String = "/book-key") {
    api<Book>(path) {
        key { call ->
            call.request.queryParameters["keyId"]?.toLong()
        }
    }
}

fun Route.batchRoutes(
    path: String = "/book-batch",
    upsert: Boolean = false,
    batchPath: String = "batch",
    deleteIdsParameterName: String = "ids",
) {
    api<Book>(path) {
        if (upsert) {
            create {
                saveMode = SaveMode.UPSERT
            }
        }
        input {
            validator {
                with(it) {
                    ::name.notBlank { "名称不能为空" }
                }
            }
            transformer {
                it.copy { name = it.name.uppercase() }
            }
        }
        batch {
            this.path = batchPath
            this.deleteIdsParameterName = deleteIdsParameterName
        }
    }
}

fun Route.customActionRoutes(path: String = "/book-custom") {
    api<Book>(path) {
        action {
            get("stats") {
                val count = sqlClient.createQuery(Book::class) {
                    select(rowCount())
                }.fetchUnlimitedCount()
                call.respond(mapOf("count" to count))
            }
            post("{id}/publish") {
                val id = call.pathParameters["id"]?.toLong()
                call.respond(mapOf("id" to id, "published" to true))
            }
        }
    }
}
