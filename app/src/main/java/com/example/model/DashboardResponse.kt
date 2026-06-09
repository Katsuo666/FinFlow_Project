package com.example.model

data class DashboardResponse(
    val balance: Double?,
    val totalIncomes: Double?,
    val totalExpenses: Double?,
    val recentTransactions: List<Expense>?
)
