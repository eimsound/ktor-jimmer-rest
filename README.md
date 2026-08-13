<h1 style="text-align: center">Ktor Jimmer Rest</h1>

A Ktor plugin that provides a concise DSL-style API for building RESTful web services
based on [Ktor](https://github.com/ktorio/ktor) and [Jimmer](https://github.com/babyfish-ct/jimmer?tab=readme-ov-file)

With one `api<T> {}` block you get the full set of CRUD endpoints for a Jimmer entity:
`GET /{id}`, `GET` (filterable, pageable list), `GET /count`, `GET /exists/{id}`, `POST`, `PUT`,
`DELETE /{id}`, plus opt-in `PATCH` and batch endpoints.

<a href="./LICENSE">
    <img src="https://img.shields.io/github/license/eimsound/ktor-jimmer-rest.svg" alt="license">
</a>
<a href="https://github.com/babyfish-ct/jimmer">
    <img src="https://img.shields.io/badge/dependency-jimmer-darkgreen" alt="jimmer">
</a>

## Features

- **Zero-boilerplate CRUD** — declare `api<Book>("/book") {}` and all routes are registered for you
- **Declarative filtering** — reuse Jimmer's Kotlin query DSL, or plug in a Jimmer `KSpecification` DTO
- **Nullable query extensions** — `eq?`, `notEq?`, `in?`, `lt?`/`gt?`/`le?`/`ge?`, `ilike?`, `between?`, `isNull` map request parameters to predicates safely
- **Dynamic sorting** — `sort()` maps `?sort=price,desc` to `orderBy`
- **Flexible projection** — choose a Jimmer `Fetcher` DSL or a generated `View` DTO
- **Input & validation** — use the entity or a Jimmer `Input` DTO, with a built-in validation DSL and transformers
- **Per-operation write config** — `create {}` / `edit {}` / `patch {}` with configurable `SaveMode` and response projection
- **Batch operations** — opt-in `POST/PUT/DELETE /{path}/batch`
- **count / exists** — always-available `GET /{path}/count` and `GET /{path}/exists/{id}`
- **Custom actions** — register arbitrary Ktor routes inside `api<T>` with `action {}`
- **Unified errors** — `ApiError` envelope + one-line `jimmerRestErrors()` wiring
- **Customizable parsing & paging** — register parsers for your own types, configure default page size and custom page objects
- **Plug & play** — everything is configured through the `JimmerRest` Ktor plugin

## Start
Add it in your settings.gradle(.kts)

```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add ``ktor-jimmer-rest`` to your project

```kotlin
implementation("com.github.eimsound:ktor-jimmer-rest")
```

Use the JimmerRest plugin in your project

```kotlin
install(JimmerRest) {
    jimmerSqlClientFactory {
        inject<KSqlClient>() // koin inject 
    }
}
```

### Configuration

The plugin exposes four configuration blocks:

```kotlin
install(JimmerRest) {
    jimmerSqlClientFactory {
        inject<KSqlClient>()
    }

    parser {
        register<IntRange> {
            val split = split("-")
            IntRange(split[0].toInt(), split[1].toInt())
        }
    }

    pager {
        defaultPageIndex = 0
        defaultPageSize = 10
        pageIndexParameterName = "pageIndex"
        pageSizeParameterName = "pageSize"
        // pageFactory = { rows, totalCount, source -> MyPage(rows, totalCount, ...) }
    }

    router {
        extParameterSeparator = "__"   // e.g. price__ge
        subParameterSeparator = "_"    // e.g. store_name
        defaultPathVariable = "{id}"
    }

    endpoint {
        batchPath = "batch"            // batch endpoints: /book/batch
        batchIdsParameterName = "ids"  // batch delete: ?ids=1,2
        sortParameterName = "sort"     // dynamic sort: ?sort=price,desc
        countPath = "count"            // count: /book/count
        existsPath = "exists/{id}"     // exists: /book/exists/{id}
    }
}
```

## Usage

`api<T>` registers all routes for the entity. For detailed usage, refer to
the [documentation](https://ktor-jimmer-rest.eimsound.github.com).

```kotlin
api<Book>("/book") {
    // use specification dto or filter dsl
    // filter(BookSpec::class)
    filter {
        where(
            `ilike?`(table::name),          // ?name__start=GraphQL
            `in?`(table::edition),          // ?edition=1,2
            `between?`(table::price)        // ?price__ge=50&price__le=80
        )
        sort()                              // ?sort=price,desc
        orderBy(table.id.desc())
    }

    // use view dto or fetcher dsl
    // fetcher(BookView::class)
    fetcher {
        fetch.by {
            allScalarFields()
            name()
            store { name(); website() }
            authors { name(); firstName(); lastName() }
        }
    }

    // use input dto or entity dsl
    // input(BookInput::class) {}
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

    // per-operation write config (C1/C4)
    create {
        saveMode = SaveMode.UPSERT          // upsert on business key
        fetcher { fetch.by { name(); edition(); price() } }  // response projection
    }
    edit {
        fetcher { fetch.by { name(); edition(); price() } }
    }
    patch { }                               // enable PATCH (own config, same semantics as PUT)
    batch { }                               // enable POST/PUT/DELETE /book/batch

    // custom actions (C7)
    action {
        get("stats") {
            val count = sqlClient.createQuery(Book::class) {
                select(rowCount())
            }.fetchUnlimitedCount()
            call.respond(mapOf("count" to count))
        }
    }
}
```

* Inside ``api<T>{}``, ``T`` is a jimmer entity class, used to mark the context type.
  The block runs **once at registration**; request-dependent parts (`filter`, `key { call -> }`) are stored as request-time lambdas.
* The filter conditions inside ``filter`` are the functions provided by jimmer, and we have added extensions to these
  functions that map request parameters to predicates automatically (see the table below).
* The fetcher continues to use the functionality of jimmer; please refer to
  [jimmer's documentation](https://babyfish-ct.github.io/jimmer-doc/zh/docs/overview/welcome) for details.
* The input includes validator and transformer, which can be used to validate and transform objects.

### Generated routes

For `api<Book>("/book")`, the following endpoints are available:

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/book/{id}` | Fetch one entity by id (404 with `ApiError` when missing) |
| `GET` | `/book` | Filterable, pageable list |
| `GET` | `/book/count` | Filtered total count |
| `GET` | `/book/exists/{id}` | `true` / `false` |
| `POST` | `/book` | Create |
| `PUT` | `/book` | Update (entity from the request body carries the id) |
| `PATCH` | `/book` | Partial update (enabled by `patch {}`) |
| `POST` | `/book/batch` | Batch create (enabled by `batch {}`) |
| `PUT` | `/book/batch` | Batch update (enabled by `batch {}`) |
| `DELETE` | `/book/batch?ids=1,2` | Batch delete (enabled by `batch {}`) |
| `DELETE` | `/book/{id}` | Delete by id |
| custom | `/book/...` | Routes registered via `action {}` |

### Query parameters

Filter predicates are bound to request parameters automatically:

| Extension | Query parameter | Example |
|-----------|-----------------|---------|
| `eq?` | `{name}` (plain, or `__exact`) | `?name=GraphQL` |
| `notEq?` | `{name}` | `?name=GraphQL` |
| `in?` / `notIn?` | `{name}` comma-separated or repeated | `?id=1,2` |
| `lt?` / `gt?` | `{name}__lt` / `{name}__gt` | `?price__lt=80` |
| `le?` / `ge?` | `{name}__le` / `{name}__ge` | `?price__ge=50` |
| `ilike?` | `{name}` + optional `__anywhere \| __exact \| __start \| __end` | `?name__start=GraphQL` |
| `between?` | `{name}__ge`, `{name}__le` | `?price__ge=50&price__le=80` |
| `isNull` / `noNull` | static predicates | — |
| nested table | sub-fields joined by `_` | `?store_name=O'REILLY` |
| `sort()` | `sort=字段,asc\|desc` (repeatable) | `?sort=price,desc&sort=id,asc` |

List endpoints accept `pageIndex` and `pageSize` (defaults: `0` and `10`, configurable via `pager`).

### Unified error responses

All errors can be returned as a consistent envelope via `jimmerRestErrors()`:

```kotlin
install(StatusPages) {
    jimmerRestErrors() // ValidationException → 400, ParseException → 400, Throwable → 500
}
```

```json
{"status": 400, "code": "BAD_REQUEST", "message": "...", "errors": ["..."]}
```

The framework's own 404 (missing `/{id}`) also returns the envelope (`NOT_FOUND`).

## Modules

| Module | Responsibility |
|--------|----------------|
| `ktor-jimmer-rest-config` | `JimmerRest` Ktor plugin, router / parser / pager / endpoint configuration |
| `ktor-jimmer-rest-route` | Route registration (`api`, `id`, `list`, `count`, `exists`, `create`, `edit`, `patch`, `remove`, batch) |
| `ktor-jimmer-rest-provider` | DSL scopes and providers (filter, fetcher, input, validator, transformer) |
| `ktor-jimmer-rest-validator` | Validation DSL, `ApiError` envelope and exception handling |
| `ktor-jimmer-rest-util` | Parameter parsing, paging helpers, Jimmer extensions |

## Sample

See [ktor-jimmer-rest-sample](https://github.com/SparrowAndSnow/ktor-jimmer-rest-sample) for a runnable
book-service / order-service example (PostgreSQL via docker-compose).

## Documentation

- [简体中文文档](https://ktor-jimmer-rest.eimsound.github.com/)
- [English Docs](https://ktor-jimmer-rest.eimsound.github.com/en/)

## License

[Apache License 2.0](./LICENSE)
