package com.example.model

data class Invoice(
    val id: Int?,
    val storeName: String,
    val amount: Double,
    val date: String,
    val itemsCount: Int
)
