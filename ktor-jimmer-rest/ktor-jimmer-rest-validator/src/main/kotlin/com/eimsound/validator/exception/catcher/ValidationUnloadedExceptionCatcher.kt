package com.eimsound.validator.exception.catcher

import com.eimsound.ktor.jimmer.rest.validator.ValidationBuilder
import org.babyfish.jimmer.UnloadedException


class ValidationUnloadedExceptionCatcher : ValidationExceptionCatcher<UnloadedException> {
    /**
     * jimmer获取为空的值会抛出 UnloadedException 需要捕获
     * @see UnloadedException
     * @param builder ValidationBuilder
     * @param e UnloadedException
     * @return ValidationBuilder
     */
    override fun handle(builder: ValidationBuilder, e: UnloadedException): ValidationBuilder {
        builder.error { "${e.type}.${e.prop} cannot be null" }
        return builder
    }
}
