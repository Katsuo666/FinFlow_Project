package com.example.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    fun formatEuro(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("pt", "PT")).apply {
            currency = java.util.Currency.getInstance("EUR")
        }.format(amount)
    }

    fun getCurrencyFormat(): NumberFormat {
        return NumberFormat.getCurrencyInstance(Locale("pt", "PT")).apply {
            currency = java.util.Currency.getInstance("EUR")
        }
    }
}
