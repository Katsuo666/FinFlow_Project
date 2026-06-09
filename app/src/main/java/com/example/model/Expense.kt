package com.example.model

data class Expense(
    val id: Int?,
    val description: String,
    val amount: Double,
    val date: String, // formato yyyy-MM-dd
    val categoryId: Int?,
    val categoryName: String? = null,
    @Transient val isIncome: Boolean = false, // Auxiliar para separar visuais locais
    val category: Category? = null // Objeto de categoria aninhado para compatibilidade com Spring Boot JPA
) {
    val displayCategoryName: String
        get() {
            val name = category?.displayName ?: categoryName
            if (name != null && name.isNotBlank() && name != "Geral") {
                val cleanName = name.lowercase().trim()
                return when {
                    cleanName == "salário" || cleanName == "vencimento" || cleanName.contains("salário") || cleanName.contains("salario") || cleanName.contains("vencimento") -> "Salário / Vencimento"
                    cleanName == "moradia" || cleanName.contains("moradia") || cleanName.contains("renda") || cleanName.contains("luz") || cleanName.contains("água") || cleanName.contains("agua") || cleanName.contains("contas") -> "Moradia (Renda/Luz/Água)"
                    cleanName == "alimentação" || cleanName.contains("alimenta") || cleanName.contains("restaurante") || cleanName.contains("supermercado") || cleanName.contains("comida") -> "Alimentação"
                    cleanName == "transporte" || cleanName.contains("transporte") || cleanName.contains("carro") || cleanName.contains("gasolina") -> "Transporte"
                    cleanName == "lazer" || cleanName.contains("lazer") || cleanName.contains("viagem") -> "Lazer"
                    cleanName == "outros" || cleanName.contains("geral") || cleanName.contains("outro") -> "Outros"
                    else -> name
                }
            }
            return when (categoryId) {
                1 -> "Salário / Vencimento"
                2 -> "Moradia (Renda/Luz/Água)"
                3 -> "Alimentação"
                4 -> "Transporte"
                5 -> "Lazer"
                6 -> "Outros"
                else -> "Outros"
            }
        }
}
