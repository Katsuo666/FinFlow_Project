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
            // Prioridade 1: ID da Categoria (Mapeamento padrão fixo 1-6)
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

            // Prioridade 2: Nome da Categoria oficial do objeto (Se vier do servidor/selecionado)
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
