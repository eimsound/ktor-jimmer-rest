<h1 style="text-align: center">Ktor Jimmer Rest</h1>

A Ktor plugin that provides a concise DSL-style API for building RESTful web services
based on [Ktor](https://github.com/ktorio/ktor) and [Jimmer](https://github.com/babyfish-ct/jimmer?tab=readme-ov-file)

With one `api<T> {}` block you get the full set of CRUD endpoints for a Jimmer entity:
`GET /{id}`, `GET` (filterable, pageable list), `POST`, `PUT`, and `DELETE /{id}`.

<a href="./LICENSE">
    <img src="https://img.shields.io/github/license/eimsound/ktor-jimmer-rest.svg" alt="license">
</a>
<a href="https://github.com/babyfish-ct/jimmer">
    <img src="https://img.shields.io/badge/dependency-jimmer-darkgreen" alt="jimmer">
</a>

## Features

- **Zero-boilerplate CRUD** — declare `api<Book>("/book") {}` and the five REST routes are registered for you
- **Declarative filtering** — reuse Jimmer's Kotlin query DSL, or plug in a Jimmer `KSpecification` DTO
- **Nullable query extensions** — `eq?`, `ilike?`, `between?`, `noNull` map request parameters to predicates safely
- **Flexible projection** — choose a Jimmer `Fetcher` DSL or a generated `View` DTO
- **Input & validation** — use the entity or a Jimmer `Input` DTO, with a built-in validation DSL and transformers
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

The plugin exposes three configuration blocks:

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
}
```

## Usage

`api<T>` registers `create | remove | edit | id | list` routes for the entity. For detailed usage, refer to
the [documentation](https://ktor-jimmer-rest.eimsound.github.com).

```kotlin
api<Book> {
    
    // use specification dto or filter dsl
    // filter(BookSpec::class)
    filter {
        where(
            `ilike?`(table::name),
            `ilike?`(table.store::name),
            `between?`(table::price),
            table.edition.`between?`(call["price", "ge"], call["price", "le"])
        )
        orderBy(table.id.desc())
    }
    
    // use view dto or fetcher dsl
    // fetcher(BookView::class)
    fetcher {
        fetch.by {
            allScalarFields()
            name()
            store {
                name()
                website()
            }
            authors {
                name()
                firstName()
                lastName()
            }
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
}
```

* Inside ``api<T>{}``, ``T`` is a jimmer entity class, used to mark the context type
* The filter conditions inside ``filter`` are the functions provided by jimmer, and we have added extensions to these
  functions,
  for example, `` `between?`(table::price) `` is nullable and will be mapped to the ``price__ge | price__le`` query
  parameter,
  and for ``__ge | __le``, it is a special extension of `` `between?` ``, `` `ilike?` `` can be used with the suffixes
  `` __anywhere | __exact | __start | __end ``, which correspond to different filtering functions, see
  the [documentation](https://ktor-jimmer-rest.eimsound.github.com) for details
* The fetcher then continues to use the functionality of jimmer, jimmer is indeed a very powerful orm framework, and writing
  it is very elegant, please refer to [jimmer's documentation](https://babyfish-ct.github.io/jimmer-doc/zh/docs/overview/welcome)
  for details
* The input includes validator and transformer, which can be used to validate and transform objects

### Generated routes

For `api<Book>("/book")`, the following endpoints are available:

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/book/{id}` | Fetch one entity by id (404 when missing) |
| `GET` | `/book` | Filterable, pageable list |
| `POST` | `/book` | Create (`INSERT_ONLY`) |
| `PUT` | `/book` | Update (`UPDATE_ONLY`, entity from the request body carries the id) |
| `DELETE` | `/book/{id}` | Delete by id |

### Query parameters

Filter predicates are bound to request parameters automatically:

| Extension | Query parameter | Example |
|-----------|-----------------|---------|
| `eq?` | `{name}` | `?name=GraphQL` |
| `ilike?` | `{name}` + optional `__anywhere \| __exact \| __start \| __end` | `?name__start=GraphQL` |
| `between?` | `{name}__ge`, `{name}__le` | `?price__ge=50&price__le=80` |
| nested table | sub-fields joined by `_` | `?store_name=O'REILLY` |

List endpoints accept `pageIndex` and `pageSize` (defaults: `0` and `10`, configurable via `pager`).

## Modules

| Module | Responsibility |
|--------|----------------|
| `ktor-jimmer-rest-config` | `JimmerRest` Ktor plugin, router / parser / pager configuration |
| `ktor-jimmer-rest-route` | Route registration (`api`, `id`, `list`, `create`, `edit`, `remove`) |
| `ktor-jimmer-rest-provider` | DSL scopes and providers (filter, fetcher, input, validator, transformer) |
| `ktor-jimmer-rest-validator` | Validation DSL and exception handling |
| `ktor-jimmer-rest-util` | Parameter parsing, paging helpers, Jimmer extensions |

## Sample

See [ktor-jimmer-rest-sample](https://github.com/SparrowAndSnow/ktor-jimmer-rest-sample) for a runnable
book-service / order-service example (PostgreSQL via docker-compose).

## Documentation

- [简体中文文档](https://ktor-jimmer-rest.eimsound.github.com/)
- [English Docs](https://ktor-jimmer-rest.eimsound.github.com/en/)

## License

[Apache License 2.0](./LICENSE)
