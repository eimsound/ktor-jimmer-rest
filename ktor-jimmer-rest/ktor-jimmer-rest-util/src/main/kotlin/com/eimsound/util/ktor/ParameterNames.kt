package com.eimsound.util.ktor

import com.eimsound.ktor.config.Configuration
import com.eimsound.util.reflect.getPropertyByPropertyName
import kotlin.jvm.internal.PropertyReference0Impl
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import org.babyfish.jimmer.sql.ast.impl.table.TableTypeProvider
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor
import org.babyfish.jimmer.sql.kt.ast.expression.KPropExpression
import java.util.concurrent.ConcurrentHashMap

/**
 * 解析"属性引用 → 查询参数名"的映射。
 *
 * 例如：
 * - 根表属性 `table::name` → `name`
 * - 嵌套关联 `table.store::name` → `store_name`（`subParameterSeparator` 默认 `_`）
 */
object ParameterNames {

    private val tableAliasGetters = ConcurrentHashMap<KClass<*>, KCallable<*>>()

    /**
     * 返回属性引用对应的查询参数名。
     *
     * @param property 必须是绑定的属性引用（如 `table::name`、`table.store::name`）
     * @throws IllegalArgumentException 属性引用未绑定（无法定位表对象）
     * @throws IllegalStateException 无法从表对象解析 Jimmer 表别名（版本兼容问题）
     */
    fun resolve(property: KProperty<*>): String = resolveWithPath(property).value

    fun resolveWithPath(property: KProperty<*>): ResolvedName {
        val receiver = boundReceiverOf(property)
        val alias = tableAliasOf(receiver)
        val segments = alias.split(".").drop(1)
        val value = (segments + property.name)
            .joinToString(Configuration.router.subParameterSeparator)
        return ResolvedName(value, segments, property.name)
    }

    /**
     * 从属性表达式（如 `table.name` / `table.store.name`）解析查询参数名。
     * 依赖 Jimmer 表达式实现保留的表引用与属性元数据。
     */
    fun resolveExpression(expression: KPropExpression<*>): ResolvedName {
        val implementor = expression.toPropExpressionImplementor()
            ?: throw IllegalArgumentException("filter 参数必须是属性表达式（如 table.name），实际是：$expression")
        val segments = associationSegmentsOf(implementor.table)
        val propertyName = implementor.prop.name
        val value = (segments + propertyName)
            .joinToString(Configuration.router.subParameterSeparator)
        return ResolvedName(value, segments, propertyName)
    }

    /**
     * 从关联表对象提取最近一层关联名（如 `table.store` → `store`）。
     * 用于 `where(table.store)` 这类以表对象为入口的关联过滤。
     *
     * 注意：依赖 Jimmer 内部接口 `KTableImplementor`/`TableImplementor.joinProp`，
     * 属于实现细节；若 Jimmer 升级改变表结构，此逻辑需重新验证。
     *
     * @throws IllegalArgumentException 表对象不是关联表（根表）或无法解析。
     */
    fun associationNameOf(table: Any): String {
        val javaTable = (table as? org.babyfish.jimmer.sql.kt.ast.table.impl.KTableImplementor<*>)
            ?.javaTable
            ?: throw IllegalArgumentException("无法解析关联名：$table 不是 Jimmer 关联表对象")
        val segments = associationSegmentsOf(javaTable)
        return segments.lastOrNull()
            ?: throw IllegalArgumentException("无法解析关联名：$table 是根表而非关联表")
    }

    private fun KPropExpression<*>.toPropExpressionImplementor(): PropExpressionImplementor<*>? =
        this as? PropExpressionImplementor<*>

    private fun associationSegmentsOf(table: Any): List<String> {
        val segments = mutableListOf<String>()
        var current: org.babyfish.jimmer.sql.ast.impl.table.TableImplementor<*>? =
            table as? org.babyfish.jimmer.sql.ast.impl.table.TableImplementor<*>
            ?: throw IllegalArgumentException(
                "无法从表对象解析关联路径：$table 不是 Jimmer 标准查询表。" +
                    "表达式必须来自查询/子查询上下文（如 table.name / table.store.name）。"
            )
        while (current != null) {
            val joinProp = current.joinProp ?: break
            segments.add(joinProp.name)
            current = current.parent
        }
        return segments.reversed()
    }

    /**
     * D3：嵌套关联的参数名与根实体的标量属性同名时，说明查询参数存在歧义，fail-fast。
     *
     * 例如根实体存在 `store_name` 属性时，`table.store::name` 生成的参数名 `store_name`
     * 无法区分是根字段还是嵌套关联，直接抛出可操作的错误。
     *
     * @param rootTable 查询的根表对象（FilterScope 的 `table`）
     * @param resolved ParameterNames.resolveWithPath 的结果
     */
    fun ensureNoRootCollision(rootTable: Any, resolved: ResolvedName) {
        if (resolved.segments.isEmpty()) {
            return
        }
        val javaTable = runCatching { javaTableOf(rootTable) }.getOrNull() ?: return
        val rootType = (javaTable as? TableTypeProvider)?.immutableType ?: return
        if (rootType.props.containsKey(resolved.value)) {
            throw IllegalStateException(
                "查询参数 '${resolved.value}' 与根实体 ${rootType} 的标量属性同名冲突 " +
                    "（嵌套路径 ${resolved.segments.joinToString(".")}.${resolved.propertyName}）。" +
                    "请调整 router.subParameterSeparator，或重命名冲突字段。"
            )
        }
    }

    private fun boundReceiverOf(property: KProperty<*>): Any {
        return (property as? PropertyReference0Impl)?.boundReceiver
            ?: throw IllegalArgumentException(
                "filter 参数必须是绑定的属性引用（如 table::name），实际是：$property"
            )
    }

    private fun tableAliasOf(receiver: Any): String {
        return javaTableOf(receiver).toString()
    }

    private fun javaTableOf(receiver: Any): Any {
        val getter = tableAliasGetters.computeIfAbsent(receiver::class) { type ->
            getPropertyByPropertyName(type, "javaTable")?.getter
                ?: throw IllegalStateException(
                    "无法从表对象 ${type.simpleName} 解析 Jimmer 表别名（缺少 javaTable 属性），" +
                        "可能与当前 Jimmer 版本不兼容，请检查依赖版本。"
                )
        }
        return checkNotNull(getter.call(receiver)) {
            "表对象 ${receiver::class.simpleName} 的 javaTable 属性值为空"
        }
    }
}

data class ResolvedName(
    val value: String,
    val segments: List<String>,
    val propertyName: String,
)
