package com.eimsound.ktor.provider

import com.eimsound.util.ktor.Pager

interface PageProvider {
    var pager: Pager
}

inline fun PageProvider.pager(block: Pager.() -> Unit) {
    block(pager)
}
