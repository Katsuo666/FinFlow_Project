package com.example.model

data class Card(
    val id: Int? = null,
    val name: String,
    val cardholderName: String,
    val lastFourDigits: String,
    val expirationDate: String,
    val type: String,
    val limitAmount: Double? = null,
    val currentBill: Double? = null,
    val brand: String
)
