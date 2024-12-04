package com.eimsound.validator.exception.catcher

import com.eimsound.ktor.jimmer.rest.validator.ValidationBuilder
import org.babyfish.jimmer.UnloadedException


class ValidationUnloadedExceptionCatcher : ValidationExceptionCatcher<UnloadedException> {
    override fun handle(builder: ValidationBuilder, e: UnloadedException): ValidationBuilder {
        builder.error { "${e.prop} cannot be null" }
        return builder
    }
}
