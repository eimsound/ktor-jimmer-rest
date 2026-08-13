package com.eimsound.rest.test.dto

import com.eimsound.rest.test.entity.Book

data class BookPageDto(
    val rows: List<Book> = emptyList(),
    val totalRowCount: Long = 0,
)
