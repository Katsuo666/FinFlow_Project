package com.example.model

import java.util.Locale

data class Category(
    val id: Int,
    val name: String
) {
    val displayName: String
        get() {
            val cleanName = name.lowercase().trim()
            return when {
                cleanName == "salário" || cleanName == "vencimento" || cleanName.contains("salário") || cleanName.contains("salario") || cleanName.contains("vencimento") -> "Salário"
                cleanName == "moradia" || cleanName.contains("moradia") || cleanName.contains("renda") || cleanName.contains("luz") || cleanName.contains("água") || cleanName.contains("agua") || cleanName.contains("contas") -> "Moradia"
                cleanName == "alimentação" || cleanName.contains("alimenta") || cleanName.contains("restaurante") || cleanName.contains("supermercado") || cleanName.contains("comida") -> "Alimentação"
                cleanName == "transporte" || cleanName.contains("transporte") || cleanName.contains("carro") || cleanName.contains("gasolina") -> "Transporte"
                cleanName == "lazer" || cleanName.contains("lazer") || cleanName.contains("viagem") -> "Lazer"
                cleanName == "outros" || cleanName.contains("geral") || cleanName.contains("outro") -> "Outros"
                else -> name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
        }
}

