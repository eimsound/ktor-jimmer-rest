# Ktor Jimmer Rest

基于 [Ktor](https://github.com/ktorio/ktor) 和 [Jimmer](https://github.com/babyfish-ct/jimmer?tab=readme-ov-file) 的快捷 Restful API 工具包。

厌烦了重复编写繁琐的 CRUD 代码？试试这个！[快速开始](./quick-start.md)

## 特性

- **一个 `api<T>` 搞定全部 CRUD**：自动注册 `GET /{id}`、`GET`（列表）、`POST`、`PUT`、`DELETE /{id}` 五条路由
- **声明式过滤**：复用 Jimmer 查询 DSL，或直接使用 `KSpecification` DTO
- **可空查询扩展**：`eq?` / `ilike?` / `between?` / `noNull` 把请求参数安全地映射为查询条件
- **灵活的投影**：Fetcher DSL 或生成的 `View` DTO 二选一
- **输入与校验**：实体或 Jimmer `Input` DTO，内置校验 DSL 与 transformer
- **可定制解析与分页**：自定义类型解析器、默认分页参数、自定义分页对象
- **即插即用**：所有配置都收敛在 `JimmerRest` Ktor 插件里

<div class="grid" markdown>

``` title="使用前"
routing {
    route("/book") {
        get("/{id}") {
            val id = call.defaultPathVariable.parse(entityIdType<Book>())
            val book = sqlClient.findById(Book::class, id)
            call.respond(book)
        }
        get {
            val pageIndex = call.request.queryParameters["pageIndex"]?.toInt() ?: 0
            val pageSize = call.request.queryParameters["pageSize"]?.toInt() ?: 10
            // ...手工拼接查询条件、排序、分页、异常处理
        }
        post { /* 接收、校验、转换、保存 */ }
        put { /* 接收、校验、转换、更新 */ }
        delete("/{id}") { /* 解析 id、删除 */ }
    }
}
```

``` title="使用后"
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
                    ::name.notBlank { "名称不能为空" }
                }
            }
        }
    }
}
```

</div>

## 文档

- [快速开始](./quick-start.md)
- [数据准备](./example/data-preparation.md)
