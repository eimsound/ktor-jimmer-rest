<h1 style="text-align: center">Ktor Jimmer Rest</h1>

A Ktor plugin that provides a concise DSL-style API for building RESTful web services
based on [Ktor](https://github.com/ktorio/ktor) and [Jimmer](https://github.com/babyfish-ct/jimmer?tab=readme-ov-file)

go to the [ktor-jimmer-rest-sample](https://github.com/SparrowAndSnow/ktor-jimmer-rest-sample)

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

## Usage

Provides routes (``create | remove | edit | id | list | api``). For detailed usage, refer to
the [documentation](https://github.com/eimsound/ktor-jimmer-rest). 

```kotlin
api<Book> {
    
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
  the [documentation](https://github.com/eimsound/ktor-jimmer-rest) for details
* The fetcher then continues to use the functionality of jimmer, jimmer is indeed a very powerful orm framework, and writing
  it is very elegant, please refer to [jimmer's documentation](https://babyfish-ct.github.io/jimmer-doc/zh/docs/overview/welcome)
  for details
* The input includes validator and transformer, which can be used to validate and transform objects