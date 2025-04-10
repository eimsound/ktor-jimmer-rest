<h1 style="text-align: center">Ktor Jimmer Rest</h1>

A Ktor plugin that provides a concise DSL-style API for building RESTful web services
based on [Ktor](https://github.com/ktorio/ktor) and [Jimmer](https://github.com/babyfish-ct/jimmer?tab=readme-ov-file)

<a href="./LICENSE">
    <img src="https://img.shields.io/github/license/eimsound/ktor-jimmer-rest.svg" alt="license">
</a>
<a href="https://github.com/babyfish-ct/jimmer">
    <img src="https://img.shields.io/badge/dependency-jimmer-darkgreen" alt="jimmer">
</a>

## Start
Add it in your settings.gradle(.kts)

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add ``ktor-jimmer-rest`` to your project

```kotlin
implementation("com.eimsound:ktor-jimmer-rest")
```

Use the JimmerRest plugin in your project

```kotlin
install(JimmerRest) {
    jimmerSqlClientFactory {
        inject<KSqlClient>()
    }
}
```

## Usage

Provides routes (``create | remove | edit | id | list | api``). For detailed usage, refer to
the [documentation](https://github.com/eimsound/ktor-jimmer-rest). Here, the ``list`` is used as an example.

```kotlin
list<Book> {
    // use specification dto or filter dsl
    // filter(BookSpec::class)
    filter {
        where(
            `ilike?`(table::name),
            `ilike?`(table.store::name),
            `between?`(table::price),
            table.edition.`between?`(get("price", "le"), this["price", "ge"])
        )
        orderBy(table.id.desc())
    }
    // use view dto or fetcher dsl
    // fetcher(BookView::class)
    fetcher {
        creator.by {
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
}
```

* Inside ``list<T>{}``, ``T`` is a jimmer entity class, used to mark the context type
* The filter conditions inside ``filter`` are the functions provided by jimmer, and we have added extensions to these
  functions,
  for example, `` `between?`(table::price) `` is nullable and will be mapped to the ``price__ge | price__le`` query
  parameter,
  and for ``__ge | __le``, it is a special extension of `` `between?` ``, `` `ilike?` `` can be used with the suffixes
  `` __anywhere | __exact | __start | __end ``, which correspond to different filtering functions, see
  the [documentation](https://github.com/eimsound/ktor-jimmer-rest) for details
* fetcher then continues to use the functionality of jimmer, jimmer is indeed a very powerful orm framework, and writing
  it is very elegant, please refer to [jimmer's documentation](https://babyfish-ct.github.io/jimmer-doc/zh/docs/overview/welcome)
  for details
