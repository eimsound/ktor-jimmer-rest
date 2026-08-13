# Ktor Jimmer Rest

A concise RESTful API toolkit built on [Ktor](https://github.com/ktorio/ktor) and
[Jimmer](https://github.com/babyfish-ct/jimmer?tab=readme-ov-file).

Tired of writing the same CRUD boilerplate over and over? Try this!
[Quick Start](./quick-start.md)

## Features

- **Full CRUD from a single `api<T>` block** — registers `GET /{id}`, `GET` (list), `POST`, `PUT` and `DELETE /{id}` routes automatically
- **Declarative filtering** — reuse Jimmer's Kotlin query DSL, or plug in a `KSpecification` DTO
- **Nullable query extensions** — `eq?`, `ilike?`, `between?`, `noNull` map request parameters to predicates safely
- **Flexible projection** — pick a `Fetcher` DSL or a generated `View` DTO
- **Input & validation** — entity or Jimmer `Input` DTO, with a built-in validation DSL and transformers
- **Customizable parsing & paging** — register parsers for your own types, configure defaults and custom page objects
- **Plug & play** — everything is configured through the `JimmerRest` Ktor plugin

<div class="grid" markdown>

``` title="Before"
routing {
    route("/book") {
        get("/{id}") {
            val id = call.defaultPathVariable.parse(entityIdType<Book>())
            val book = sqlClient.findById(Book::class, id)
            call.respond(book)
        }
        get {
            // ...manually assemble filters, sorting, paging and error handling
        }
        post { /* receive, validate, transform, save */ }
        put { /* receive, validate, transform, update */ }
        delete("/{id}") { /* parse id, delete */ }
    }
}
```

``` title="After"
routing {
    api<Book>("/book") {
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
                store { name(); website() }
            }
        }
        input {
            validator {
                with(it) {
                    ::name.notBlank { "name must not be blank" }
                }
            }
        }
    }
}
```

</div>

## Documentation

- [Quick Start](./quick-start.md)
- [Data Preparation](../zh/example/data-preparation.md) (简体中文)
