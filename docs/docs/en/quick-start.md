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

A single `api<Book>("/book")` registers five REST routes:

```kotlin
routing {
    api<Book>("/book") {
        // Filtering: query parameters map to where conditions automatically
        filter {
            where(
                `ilike?`(table::name),
                `between?`(table::price)
            )
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
    }
}
```

## 5. Generated endpoints

`api<Book>("/book")` registers the following endpoints:

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/book/{id}` | Fetch one entity by id (404 when missing) |
| `GET` | `/book` | Filterable, pageable list |
| `POST` | `/book` | Create (`INSERT_ONLY`) |
| `PUT` | `/book` | Update (`UPDATE_ONLY`, the body carries the id) |
| `DELETE` | `/book/{id}` | Delete by id |

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
| `eq?` | `{field}` | `?name=GraphQL` |
| `ilike?` | `{field}` + `__anywhere` / `__exact` / `__start` / `__end` | `?name__start=GraphQL` |
| `between?` | `{field}__ge`, `{field}__le` | `?price__ge=50&price__le=80` |
| Association fields | child table and field joined with `_` | `?store_name=O'REILLY` |

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

## 7. Validation & error handling

Validation failures throw `ValidationException`. Handle it centrally with `StatusPages`:

```kotlin
install(StatusPages) {
    exception<ValidationException> { call, cause ->
        call.respond(cause.httpStatusCode, cause.errors)
    }

    exception<ParseException> { call, cause ->
        call.respondText(cause.message, status = HttpStatusCode.BadRequest)
    }
}
```

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
