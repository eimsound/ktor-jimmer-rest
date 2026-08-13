# 快速开始

本指南以 Jimmer 的经典 `Book` 实体为例，演示如何从一个空项目起步，把 CRUD 接口跑起来。

## 环境要求

- JDK 17+
- Gradle 8+（或直接使用仓库自带的 wrapper）
- 可选：Docker（用于启动示例项目的 PostgreSQL 数据库）

## 1. 引入依赖

在 `settings.gradle.kts` 中添加 JitPack 仓库：

```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
        mavenCentral()
    }
}
```

在 `build.gradle.kts` 中添加依赖：

```kotlin
dependencies {
    implementation("com.github.eimsound:ktor-jimmer-rest")
    // Jimmer KSP 代码生成（定义实体/DTO 时必需）
    ksp("org.babyfish.jimmer:jimmer-ksp:0.11.5")
}
```

## 2. 安装插件

在 Ktor 应用的 `Application.module()` 中安装 `JimmerRest`，并注入 `KSqlClient`：

```kotlin
fun Application.configureFrameworks() {
    install(JimmerRest) {
        jimmerSqlClientFactory {
            inject<KSqlClient>() // 这里是 Koin 注入示例，也可以是任何提供 KSqlClient 的方式
        }
    }
}
```

## 3. 定义实体

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

## 4. 声明 API

一行 `api<Book>("/book")` 即可注册全套 REST 路由（block 只在注册时执行一次）：

```kotlin
routing {
    api<Book>("/book") {
        // 过滤：查询参数会自动映射到 where 条件
        filter {
            where(
                `ilike?`(table::name),
                `in?`(table::edition),
                `between?`(table::price)
            )
            sort()
            orderBy(table.id.desc())
        }

        // 投影：只返回需要的字段
        fetcher {
            fetch.by {
                allScalarFields()
                store {
                    name()
                    website()
                }
            }
        }

        // 输入：创建/更新时校验和转换
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

        // 写操作独立配置：保存模式 + 响应投影
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
        patch { }   // 启用 PATCH 部分更新
        batch { }   // 启用批量端点

        // 自定义动作
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

## 5. 生成的接口

`api<Book>("/book")` 会注册以下端点：

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/book/{id}` | 按 id 查询单个实体，不存在返回 404 |
| `GET` | `/book` | 过滤 + 分页的列表查询 |
| `GET` | `/book/count` | 过滤后的总数 |
| `GET` | `/book/exists/{id}` | 是否存在（true/false） |
| `POST` | `/book` | 创建（`INSERT_ONLY`） |
| `PUT` | `/book` | 更新（`UPDATE_ONLY`，请求体携带 id） |
| `PATCH` | `/book` | 部分更新（`patch {}` 启用，缺省字段不覆盖） |
| `POST` | `/book/batch` | 批量创建（`batch {}` 启用） |
| `PUT` | `/book/batch` | 批量更新（`batch {}` 启用） |
| `DELETE` | `/book/batch?ids=1,2` | 批量删除（`batch {}` 启用） |
| `DELETE` | `/book/{id}` | 按 id 删除 |
| 自定义 | `/book/...` | `action {}` 注册的路由 |

```bash
# 创建
curl -X POST http://localhost:8081/book \
  -H "Content-Type: application/json" \
  -d '{"name":"Learning GraphQL","edition":1,"price":50}'

# 查询单个
curl http://localhost:8081/book/1

# 列表 + 过滤 + 分页
curl "http://localhost:8081/book?name__start=GraphQL&price__ge=50&pageIndex=0&pageSize=10"

# 计数 / 存在性
curl http://localhost:8081/book/count
curl http://localhost:8081/book/exists/1

# 更新
curl -X PUT http://localhost:8081/book \
  -H "Content-Type: application/json" \
  -d '{"id":1,"name":"Learning GraphQL","edition":1,"price":55}'

# 部分更新（PATCH）
curl -X PATCH http://localhost:8081/book \
  -H "Content-Type: application/json" \
  -d '{"id":1,"price":55}'

# 批量创建 / 批量删除
curl -X POST http://localhost:8081/book/batch \
  -H "Content-Type: application/json" \
  -d '[{"name":"A","edition":1,"price":50},{"name":"B","edition":1,"price":60}]'
curl -X DELETE "http://localhost:8081/book/batch?ids=1,2"

# 删除
curl -X DELETE http://localhost:8081/book/1
```

## 6. 查询参数与分页

filter 中的扩展函数会自动把查询参数映射为条件：

| 扩展函数 | 查询参数 | 示例 |
|----------|----------|------|
| `eq?` | `{字段名}`（无后缀优先，其次 `__exact`） | `?name=GraphQL` |
| `notEq?` | `{字段名}` | `?name=GraphQL` |
| `in?` / `notIn?` | `{字段名}` 逗号分隔或重复参数 | `?id=1,2` |
| `lt?` / `gt?` | `{字段名}__lt` / `{字段名}__gt` | `?price__lt=80` |
| `le?` / `ge?` | `{字段名}__le` / `{字段名}__ge` | `?price__ge=50` |
| `ilike?` | `{字段名}` + `__anywhere` / `__exact` / `__start` / `__end` | `?name__start=GraphQL` |
| `between?` | `{字段名}__ge`、`{字段名}__le` | `?price__ge=50&price__le=80` |
| `isNull` / `noNull` | 静态谓词 | — |
| 关联表字段 | 子表名与字段名用 `_` 连接 | `?store_name=O'REILLY` |
| `sort()` | `sort=字段,asc\|desc`（可重复） | `?sort=price,desc&sort=id,asc` |

参数分隔符可以通过 `router` 配置修改：

```kotlin
install(JimmerRest) {
    router {
        extParameterSeparator = "__"
        subParameterSeparator = "_"
        defaultPathVariable = "{id}"
    }
}
```

列表接口默认分页参数为 `pageIndex`（默认 0）和 `pageSize`（默认 10），可通过 `pager` 配置调整，也可以用 `pageFactory` 返回自定义分页结构：

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

端点路径与查询参数名等字面量统一收进 `endpoint` 配置：

```kotlin
install(JimmerRest) {
    endpoint {
        batchPath = "batch"              // 批量端点路径
        batchIdsParameterName = "ids"    // 批量删除参数名
        sortParameterName = "sort"       // 动态排序参数名
        countPath = "count"              // 计数端点路径
        existsPath = "exists/{id}"       // 存在性端点路径
    }
}
```

## 7. 校验与异常处理

校验失败会抛出 `ValidationException`，参数解析失败抛 `ParseException`。用一行 `jimmerRestErrors()` 即可统一为 `ApiError` envelope：

```kotlin
install(StatusPages) {
    jimmerRestErrors() // ValidationException → 400、ParseException → 400、Throwable → 500
}
```

```json
{"status": 400, "code": "BAD_REQUEST", "message": "...", "errors": ["..."]}
```

框架自身的 404（`GET /book/{id}` 不存在）也会返回 envelope（`code: NOT_FOUND`）。

Jimmer 读取未加载字段时抛出的 `UnloadedException` 已被默认的 catcher 捕获并转为校验错误。

## 8. 运行示例项目

仓库中的 [ktor-jimmer-rest-sample](https://github.com/SparrowAndSnow/ktor-jimmer-rest-sample) 提供了完整的 book-service / order-service 示例：

```bash
# 1. 启动 PostgreSQL（并执行 init-postgres.sql 初始化表结构）
docker compose -f .devcontainer/docker-compose.yml up -d db

# 2. 启动 book-service（默认端口 8081）
cd sample
./gradlew :book-service:run
```

完整的建表 SQL（PostgreSQL / MySQL 两版）见[数据准备](./example/data-preparation.md)。
