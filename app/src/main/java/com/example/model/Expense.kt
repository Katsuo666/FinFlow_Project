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
            // Prioridade 1: ID da Categoria (Mapeamento mais fiável)
            val idMapping = when (categoryId) {
                1 -> "Salário / Vencimento"
                2 -> "Moradia (Renda/Luz/Água)"
                3 -> "Alimentação"
                4 -> "Transporte"
                5 -> "Lazer"
                6 -> "Outros"
                else -> null
            }
            if (idMapping != null) return idMapping

            // Prioridade 2: Nome da Categoria oficial do objeto
            val name = category?.displayName ?: categoryName
            if (name != null && name.isNotBlank() && name != "Geral" && name != "Outros") {
                val cleanName = name.lowercase().trim()
                return when {
                    cleanName.contains("salário") || cleanName.contains("salario") || cleanName.contains("vencimento") || cleanName.contains("renda") -> "Salário / Vencimento"
                    cleanName.contains("moradia") || cleanName.contains("aluguer") || cleanName.contains("luz") || cleanName.contains("água") || cleanName.contains("agua") || cleanName.contains("contas") -> "Moradia (Renda/Luz/Água)"
                    cleanName.contains("alimentação") || cleanName.contains("alimenta") || cleanName.contains("restaurante") || cleanName.contains("supermercado") || cleanName.contains("comida") || cleanName.contains("mercado") -> "Alimentação"
                    cleanName.contains("transporte") || cleanName.contains("carro") || cleanName.contains("gasolina") -> "Transporte"
                    cleanName.contains("lazer") || cleanName.contains("viagem") || cleanName.contains("diversão") -> "Lazer"
                    else -> name
                }
            }

            // Prioridade 3: Análise da Descrição (Apenas se a categoria for incerta)
            val descClean = description.lowercase().trim()
            return when {
                descClean.contains("salário") || descClean.contains("vencimento") || descClean.contains("renda") -> "Salário / Vencimento"
                descClean.contains("aluguer") || descClean.contains("contas") || descClean.contains("internet") -> "Moradia (Renda/Luz/Água)"
                descClean.contains("mercado") || descClean.contains("jantar") || descClean.contains("almoço") || descClean.contains("comida") -> "Alimentação"
                descClean.contains("gasolina") || descClean.contains("uber") || descClean.contains("autocarro") -> "Transporte"
                descClean.contains("cinema") || descClean.contains("festa") || descClean.contains("viagem") || descClean.contains("fim de semana") -> "Lazer"
                else -> "Outros"
            }
        }
}
