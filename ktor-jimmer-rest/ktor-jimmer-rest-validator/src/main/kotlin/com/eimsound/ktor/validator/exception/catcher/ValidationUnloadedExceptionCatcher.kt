package com.eimsound.ktor.validator.exception.catcher

import com.eimsound.ktor.validator.ValidationBuilder
import org.babyfish.jimmer.UnloadedException


object ValidationUnloadedExceptionCatcher : ValidationExceptionCatcher<UnloadedException> {
    /**
     * jimmer获取为空的值会抛出 UnloadedException 需要捕获
     * @see UnloadedException
     * @param builder ValidationBuilder
     * @param e UnloadedException
     * @return ValidationBuilder
     */
    override fun handle(builder: ValidationBuilder, e: UnloadedException): ValidationBuilder {
        builder.error { "${e.prop} cannot be null" }
        return builder
    }
}
