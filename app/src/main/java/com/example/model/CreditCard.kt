package com.example.model

data class CreditCard(
    val id: Int? = null,
    val name: String,
    val cardholderName: String,
    val lastFourDigits: String,
    val expirationDate: String,
    val type: String, // "CREDIT" ou "DEBIT" (CRÉDITO ou DÉBITO)
    val limitAmount: Double? = null,
    val currentBill: Double? = null,
    val brand: String // "VISA", "MASTERCARD", "OTHER" (OUTRO)
)
