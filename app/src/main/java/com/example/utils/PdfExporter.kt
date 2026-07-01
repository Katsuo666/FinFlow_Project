package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

object PdfExporter {
    fun generateAndSharePDF(
        context: Context,
        userName: String?,
        userEmail: String?,
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
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.color = "#00C853".toColorInt()
        paint.isFakeBoldText = true
        paint.textSize = 40f
        canvas.drawText("$", 50f, 60f, paint)

        paint.color = android.graphics.Color.BLACK
        paint.textSize = 28f
        canvas.drawText("FinFlow Financial Report", 100f, 55f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        canvas.drawText("Utilizador: ${userName ?: "Utilizador"}", 50f, 100f, paint)
        canvas.drawText("Email: ${userEmail ?: "N/A"}", 50f, 120f, paint)
        canvas.drawText("Data de Emissão: ${LocalDate.now()}", 50f, 140f, paint)

        paint.strokeWidth = 2f
        canvas.drawLine(50f, 155f, 545f, 155f, paint)

        paint.isFakeBoldText = true
        paint.textSize = 18f
        canvas.drawText("RESUMO FINANCEIRO", 50f, 190f, paint)
        
        paint.textSize = 14f
        paint.isFakeBoldText = false
        canvas.drawText("Total Entradas:", 50f, 220f, paint)
        canvas.drawText("$incomes €", 200f, 220f, paint)
        
        canvas.drawText("Total Saídas:", 50f, 240f, paint)
        canvas.drawText("$expenses €", 200f, 240f, paint)
        
        paint.isFakeBoldText = true
        canvas.drawText("Saldo Líquido:", 50f, 260f, paint)
        canvas.drawText("${incomes - expenses} €", 200f, 260f, paint)

        paint.strokeWidth = 1f
        canvas.drawLine(50f, 280f, 545f, 280f, paint)

        paint.isFakeBoldText = true
        paint.textSize = 18f
        canvas.drawText("DISTRIBUIÇÃO POR CATEGORIA", 50f, 310f, paint)
        
        paint.textSize = 14f
        paint.isFakeBoldText = false
        val startY = 340f
        val spacing = 25f
        canvas.drawText("• Moradia:", 50f, startY, paint); canvas.drawText(moradia, 200f, startY, paint)
        canvas.drawText("• Alimentação:", 50f, startY + spacing, paint); canvas.drawText(alimentacao, 200f, startY + spacing, paint)
        canvas.drawText("• Transporte:", 50f, startY + 2 * spacing, paint); canvas.drawText(transporte, 200f, startY + 2 * spacing, paint)
        canvas.drawText("• Lazer:", 50f, startY + 3 * spacing, paint); canvas.drawText(lazer, 200f, startY + 3 * spacing, paint)
        canvas.drawText("• Saúde:", 50f, startY + 4 * spacing, paint); canvas.drawText(saude, 200f, startY + 4 * spacing, paint)
        canvas.drawText("• Salário:", 50f, startY + 5 * spacing, paint); canvas.drawText(salario, 200f, startY + 5 * spacing, paint)
        canvas.drawText("• Outros:", 50f, startY + 6 * spacing, paint); canvas.drawText(outros, 200f, startY + 6 * spacing, paint)

        paint.textSize = 10f
        paint.color = android.graphics.Color.GRAY
        canvas.drawText("Gerado automaticamente pela App FinFlow Mobile", 50f, 800f, paint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Relatorio_FinFlow.pdf")
        try {
            if (file.exists()) file.delete()
            pdfDocument.writeTo(FileOutputStream(file))
            shareFile(context, file, "application/pdf")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao gerar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}

fun shareFile(context: Context, file: File, mimeType: String) {
    try {
        val authority = "com.finflow.project.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Enviar Relatório"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Erro ao partilhar ficheiro: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
