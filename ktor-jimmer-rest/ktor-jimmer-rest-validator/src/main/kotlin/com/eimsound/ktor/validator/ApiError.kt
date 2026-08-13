package com.eimsound.ktor.validator

import com.eimsound.ktor.validator.exception.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.http.parsing.ParseException
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.response.respond

/**
 * 统一错误响应结构。
 *
 * @param status HTTP 状态码
 * @param code 机器可读错误码（如 `NOT_FOUND`、`BAD_REQUEST`、`INTERNAL_ERROR`）
 * @param message 人类可读错误信息
 * @param errors 明细错误列表（如校验错误）
 */
data class ApiError(
    val status: Int,
    val code: String,
    val message: String,
    val errors: List<String> = emptyList(),
) {
    companion object {
        fun notFound(message: String = "Not Found") = ApiError(404, "NOT_FOUND", message)

        fun badRequest(message: String, errors: List<String> = emptyList()) =
            ApiError(400, "BAD_REQUEST", message, errors)

        fun internal(message: String? = null) =
            ApiError(500, "INTERNAL_ERROR", message ?: "Internal Server Error")
    }
}

/**
 * 一行接线统一错误处理：`ValidationException` → 400、`ParseException` → 400、
 * 其他 `Throwable` → 500，全部输出 [ApiError] envelope。
 *
 * 注意：若需要自定义特定异常的处理，请在本函数之前注册（StatusPages 按注册顺序匹配）。
 */
fun StatusPagesConfig.jimmerRestErrors() {
    exception<ValidationException> { call, cause ->
        call.respond(
            cause.httpStatusCode,
            ApiError.badRequest(cause.errors.joinToString(), cause.errors)
        )
    }
    exception<ParseException> { call, cause ->
        call.respond(HttpStatusCode.BadRequest, ApiError.badRequest(cause.message ?: "Bad Request"))
    }
    exception<Throwable> { call, cause ->
        call.respond(HttpStatusCode.InternalServerError, ApiError.internal(cause.message))
    }
}
