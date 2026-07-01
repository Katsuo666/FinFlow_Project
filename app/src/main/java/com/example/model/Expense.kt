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
            val idMapping = when (categoryId) {
                1 -> "Salário"
                2 -> "Moradia"
                3 -> "Alimentação"
                4 -> "Transporte"
                5 -> "Lazer"
                6 -> "Outros"
                else -> null
            }
            if (idMapping != null) return idMapping

            val name = category?.displayName ?: categoryName
            if (!name.isNullOrBlank()) {
                val cleanName = name.lowercase().trim()
                return when {
                    cleanName.contains("salário") || cleanName.contains("salario") || cleanName.contains("vencimento") -> "Salário"
                    cleanName.contains("moradia") || cleanName.contains("renda") || cleanName.contains("luz") || cleanName.contains("água") -> "Moradia"
                    cleanName.contains("alimentação") || cleanName.contains("alimenta") || cleanName.contains("restaurante") || cleanName.contains("comida") -> "Alimentação"
                    cleanName.contains("transporte") || cleanName.contains("carro") || cleanName.contains("uber") -> "Transporte"
                    cleanName.contains("lazer") || cleanName.contains("viagem") -> "Lazer"
                    cleanName == "outros" || cleanName == "geral" -> "Outros"
                    else -> name
                }
            }

            return "Outros"
        }
}
