package com.example.model

data class Expense(
    val id: Int?,
    val description: String,
    val amount: Double,
    val date: String,
    val categoryId: Int?,
    val categoryName: String? = null,
    @Transient val isIncome: Boolean = false,
    val category: Category? = null
) {
    val displayCategoryName: String
        get() {
            val name = category?.displayName ?: categoryName
            if (!name.isNullOrBlank()) return name

            return when (categoryId) {
                1 -> "Salário"
                2 -> "Moradia"
                3 -> "Alimentação"
                4 -> "Transporte"
                5 -> "Lazer"
                6 -> "Outros"
                else -> "Outros"
            }
        }
}
