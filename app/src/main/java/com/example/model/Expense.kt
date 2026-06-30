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
                    cleanName.contains("salário") || cleanName.contains("salario") || cleanName.contains("vencimento") || cleanName.contains("renda") || cleanName.contains("venda") || cleanName.contains("pagamento") -> "Salário"
                    cleanName.contains("moradia") || cleanName.contains("aluguer") || cleanName.contains("luz") || cleanName.contains("água") || cleanName.contains("agua") || cleanName.contains("contas") || cleanName.contains("internet") -> "Moradia"
                    cleanName.contains("alimentação") || cleanName.contains("alimenta") || cleanName.contains("restaurante") || cleanName.contains("supermercado") || cleanName.contains("comida") || cleanName.contains("mercado") || cleanName.contains("mercearia") || cleanName.contains("padaria") -> "Alimentação"
                    cleanName.contains("transporte") || cleanName.contains("carro") || cleanName.contains("gasolina") || cleanName.contains("combustível") || cleanName.contains("uber") -> "Transporte"
                    cleanName.contains("lazer") || cleanName.contains("viagem") || cleanName.contains("diversão") || cleanName.contains("cinema") -> "Lazer"
                    cleanName == "outros" || cleanName == "geral" -> "Outros"
                    else -> name
                }
            }

            // Prioridade 3: Análise da Descrição (Apenas se a categoria for COMPLETAMENTE nula)
            val descClean = description.lowercase().trim()
            return when {
                descClean.contains("salário") || descClean.contains("salario") || descClean.contains("vencimento") || descClean.contains("renda") || descClean.contains("venda") || descClean.contains("recebido") -> "Salário"
                descClean.contains("aluguer") || descClean.contains("contas") || descClean.contains("internet") || descClean.contains("condomínio") || descClean.contains("luz") || descClean.contains("água") || descClean.contains("eletricidade") || descClean.contains("energia") -> "Moradia"
                descClean.contains("mercado") || descClean.contains("jantar") || descClean.contains("almoço") || descClean.contains("comida") || descClean.contains("pão") || descClean.contains("óleo") || descClean.contains("arroz") || descClean.contains("batata") -> "Alimentação"
                descClean.contains("gasolina") || descClean.contains("uber") || descClean.contains("autocarro") || descClean.contains("combustível") -> "Transporte"
                descClean.contains("cinema") || descClean.contains("festa") || descClean.contains("viagem") || descClean.contains("fim de semana") || descClean.contains("concerto") -> "Lazer"
                else -> "Outros"
            }
        }
}
