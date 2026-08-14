# Quick Start

This guide walks through building a RESTful CRUD API for the classic Jimmer `Book` entity,
from an empty project to working endpoints.

## Prerequisites

- JDK 17+
- Gradle 8+ (or use the wrapper included in the sample repository)
- Optional: Docker (to start the PostgreSQL database used by the sample)

## 1. Add the dependency

Add the JitPack repository in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
        mavenCentral()
    }
}
```

Add the dependency in `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.eimsound:ktor-jimmer-rest")
    // Jimmer KSP code generation (required for entities / DTOs)
    ksp("org.babyfish.jimmer:jimmer-ksp:0.11.5")
}
```

## 2. Install the plugin

Install `JimmerRest` in your Ktor application and provide a `KSqlClient`:

```kotlin
fun Application.configureFrameworks() {
    install(JimmerRest) {
        jimmerSqlClientFactory {
            inject<KSqlClient>() // Koin injection here; use whatever provides your KSqlClient
        }
    }
}
```

## 3. Define an entity

```kotlin
@MappedSuperclass
interface BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long
}

@Entity
interface Book : BaseEntity {
    @Key
    val name: String

    @Key
    val edition: Int

    val price: BigDecimal

    @ManyToOne
    val store: BookStore?
}
```

## 4. Declare the API

A single `api<Book>("/book")` registers the full set of REST routes (the block runs once at registration):

```kotlin
routing {
    api<Book>("/book") {
        // Filtering: query parameters map to where conditions automatically
        filter {
            where(
                `ilike?`(table.name),
                `in?`(table.edition),
                `between?`(table.price)
            )
            // association filtering (EXISTS semantics, pagination-safe)
            where(table.store) {           // reference association
                `ilike?`(table.name)       // ?store_name__start=Amazon
            }
            where(Book::authors) {         // collection association
                `ilike?`(table.firstName)  // ?authors_firstName__start=Alex
            }
            sort()
            orderBy(table.id.desc())
        }

        // Projection: return only the fields you need
        fetcher {
            fetch.by {
                allScalarFields()
                store {
                    name()
                    website()
                }
            }
        }

        // Input: validate and transform on create / update
        input {
            validator {
                with(it) {
                    ::name.notBlank { "name must not be blank" }
                    ::price.range(0.toBigDecimal()..100.toBigDecimal()) { range ->
                        "price must be between ${range.start} and ${range.endInclusive}"
                    }
                }
            }
            transformer {
                it.copy { name = it.name.uppercase() }
            }
        }

        // Per-operation write config: save mode + response projection
        create {
            saveMode = SaveMode.UPSERT
            fetcher {
                fetch.by {
                    name()
                    edition()
                    price()
                }
            }
        }
        edit {
            fetcher {
                fetch.by {
                    name()
                    edition()
                    price()
                }
            }
        }
        patch { }   // enable PATCH partial update
        batch { }   // enable batch endpoints

        // Custom actions
        action {
            get("stats") {
                val count = sqlClient.createQuery(Book::class) {
                    select(rowCount())
                }.fetchUnlimitedCount()
                call.respond(mapOf("count" to count))
            }
        }
    }
}
```

## 5. Generated endpoints

`api<Book>("/book")` registers the following endpoints:

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/book/{id}` | Fetch one entity by id (404 when missing) |
| `GET` | `/book` | Filterable, pageable list |
| `GET` | `/book/count` | Filtered total count |
| `GET` | `/book/exists/{id}` | Whether the entity exists (`true`/`false`) |
| `POST` | `/book` | Create (`INSERT_ONLY`) |
| `PUT` | `/book` | Update (`UPDATE_ONLY`, the body carries the id) |
| `PATCH` | `/book` | Partial update (enabled by `patch {}`) |
| `POST` | `/book/batch` | Batch create (enabled by `batch {}`) |
| `PUT` | `/book/batch` | Batch update (enabled by `batch {}`) |
| `DELETE` | `/book/batch?ids=1,2` | Batch delete (enabled by `batch {}`) |
| `DELETE` | `/book/{id}` | Delete by id |
| custom | `/book/...` | Routes registered via `action {}` |

```bash
# Create
curl -X POST http://localhost:8081/book \
  -H "Content-Type: application/json" \
  -d '{"name":"Learning GraphQL","edition":1,"price":50}'

# Fetch one
curl http://localhost:8081/book/1

# List with filters and paging
curl "http://localhost:8081/book?name__start=GraphQL&price__ge=50&pageIndex=0&pageSize=10"

# Update
curl -X PUT http://localhost:8081/book \
  -H "Content-Type: application/json" \
  -d '{"id":1,"name":"Learning GraphQL","edition":1,"price":55}'

# Delete
curl -X DELETE http://localhost:8081/book/1
```

## 6. Query parameters & paging

Extension functions inside `filter` bind request parameters to predicates:

| Extension | Query parameter | Example |
|-----------|-----------------|---------|
| `eq?` | `{field}` (plain first, then `__exact`) | `?name=GraphQL` |
| `notEq?` | `{field}` | `?name=GraphQL` |
| `in?` / `notIn?` | `{field}` comma-separated or repeated | `?id=1,2` |
| `lt?` / `gt?` | `{field}__lt` / `{field}__gt` | `?price__lt=80` |
| `le?` / `ge?` | `{field}__le` / `{field}__ge` | `?price__ge=50` |
| `ilike?` | `{field}` + `__anywhere` / `__exact` / `__start` / `__end` | `?name__start=GraphQL` |
| `between?` | `{field}__ge`, `{field}__le` | `?price__ge=50&price__le=80` |
| `isNull` / `noNull` | static predicates | — |
| Association (EXISTS) | association path joined with `_` | `?store_books_name=GraphQL` |
| `sort()` | `sort=field,asc\|desc` (repeatable) | `?sort=price,desc&sort=id,asc` |

Separators are configurable via `router`:

```kotlin
install(JimmerRest) {
    router {
        extParameterSeparator = "__"
        subParameterSeparator = "_"
        defaultPathVariable = "{id}"
    }
}
```

List endpoints accept `pageIndex` (default 0) and `pageSize` (default 10). Configure them via `pager`,
or return a custom page structure with `pageFactory`:

```kotlin
install(JimmerRest) {
    pager {
        defaultPageIndex = 0
        defaultPageSize = 10
        pageIndexParameterName = "pageIndex"
        pageSizeParameterName = "pageSize"
        // pageFactory = { rows, totalCount, source -> MyPage(rows, totalCount, ...) }
    }
}
```

Endpoint paths and query parameter names are centralized in `endpoint`:

```kotlin
install(JimmerRest) {
    endpoint {
        batchPath = "batch"              // batch endpoint path
        batchIdsParameterName = "ids"    // batch delete ids parameter
        sortParameterName = "sort"       // dynamic sort parameter
        countPath = "count"              // count endpoint path
        existsPath = "exists/{id}"       // exists endpoint path
    }
}
```

## 7. Validation & error handling

Validation failures throw `ValidationException`; parse failures throw `ParseException`.
Wire a consistent `ApiError` envelope with a single call:

```kotlin
install(StatusPages) {
    jimmerRestErrors() // ValidationException → 400, ParseException → 400, Throwable → 500
}
```

```json
{"status": 400, "code": "BAD_REQUEST", "message": "...", "errors": ["..."]}
```

The framework's own 404 (missing `/{id}`) also returns the envelope (`code: NOT_FOUND`).

Jimmer's `UnloadedException` (reading an unloaded field) is caught by the default catcher
and turned into a validation error.

## 8. Run the sample

The [ktor-jimmer-rest-sample](https://github.com/SparrowAndSnow/ktor-jimmer-rest-sample) repository
contains a complete book-service / order-service example:

```bash
# 1. Start PostgreSQL (init-postgres.sql creates the schema)
docker compose -f .devcontainer/docker-compose.yml up -d db

# 2. Start book-service (default port 8081)
cd sample
./gradlew :book-service:run
```

See [Data Preparation](../zh/example/data-preparation.md) (简体中文) for the full DDL (PostgreSQL / MySQL).
