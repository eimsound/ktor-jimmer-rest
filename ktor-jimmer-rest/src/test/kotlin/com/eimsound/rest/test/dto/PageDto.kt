package com.eimsound.rest.test.dto

import com.eimsound.rest.test.entity.Book
import com.eimsound.rest.test.entity.OrderItem

data class BookPageDto(
    val rows: List<Book> = emptyList(),
    val totalRowCount: Long = 0,
)

data class OrderItemPageDto(
    val rows: List<OrderItem> = emptyList(),
    val totalRowCount: Long = 0,
)
