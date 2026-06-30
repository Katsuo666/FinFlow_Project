package com.example.utils

import android.content.Context
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

object ExcelExporter {
    fun generateAndShareExcel(
        context: Context,
        incomes: Double,
        expenses: Double,
        moradia: String,
        alimentacao: String,
        transporte: String,
        lazer: String,
        saude: String,
        salario: String,
        outros: String
    ) {
        // Gerar um CSV (compatível com Excel)
        val csvContent = StringBuilder()
        csvContent.append("Categoria;Valor\n")
        csvContent.append("Total Entradas;$incomes\n")
        csvContent.append("Total Saidas;$expenses\n")
        csvContent.append("Saldo Liquido;${incomes - expenses}\n")
        csvContent.append("\n")
        csvContent.append("DETALHE POR CATEGORIA\n")
        csvContent.append("Moradia;${moradia}\n")
        csvContent.append("Alimentacao;${alimentacao}\n")
        csvContent.append("Transporte;${transporte}\n")
        csvContent.append("Lazer;${lazer}\n")
        csvContent.append("Saude;${saude}\n")
        csvContent.append("Salario;${salario}\n")
        csvContent.append("Outros;${outros}\n")

        val file = File(context.cacheDir, "Relatorio_FinFlow.csv")
        try {
            if (file.exists()) file.delete()
            FileOutputStream(file).use { it.write(csvContent.toString().toByteArray()) }
            shareFile(context, file, "text/csv")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao gerar Excel/CSV: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
