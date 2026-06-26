package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.Expense
import com.example.utils.TokenManager
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

// ---------------- ECRÃ DO SEPARADOR RELATÓRIOS ----------------
data class ChartPoint(val label: String, val income: Double, val expense: Double)

@Composable
fun RelatoriosScreen(allIncomes: List<Expense>, allExpenses: List<Expense>) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "PT")).apply {
        currency = java.util.Currency.getInstance("EUR")
    }

    val totalIncomes = allIncomes.sumOf { it.amount }
    val totalExpenses = allExpenses.sumOf { it.amount }

    var selectedPeriod by remember { mutableStateOf("6 Meses") }
    val periods = listOf("1 Semana", "1 Mês", "3 Meses", "6 Meses", "1 Ano")

    val today = remember {
        try {
            java.time.LocalDate.now()
        } catch (_: Exception) {
            java.time.LocalDate.of(2026, 6, 8)
        }
    }

    val parseLocalDate = { dateStr: String ->
        try {
            java.time.LocalDate.parse(dateStr)
        } catch (_: Exception) {
            null
        }
    }

    // Lógica de agrupamento simplificada usando a inteligência do modelo Expense
    val groupedData = remember(allExpenses, allIncomes) {
        val map = mutableMapOf<String, Double>()
        
        allExpenses.forEach { tx ->
            val cat = tx.displayCategoryName
            map[cat] = map.getOrDefault(cat, 0.0) + tx.amount
        }
        
        allIncomes.forEach { tx ->
            val cat = tx.displayCategoryName
            if (cat.contains("Salário") || cat.contains("Vencimento")) {
                map["Salário"] = map.getOrDefault("Salário", 0.0) + tx.amount
            }
        }
        map
    }

    val moradiaAmt = groupedData.entries.asSequence().filter { it.key.contains("Moradia") }.sumOf { it.value }
    val alimentacaoAmt = groupedData.getOrDefault("Alimentação", 0.0)
    val transporteAmt = groupedData.getOrDefault("Transporte", 0.0)
    val lazerAmt = groupedData.getOrDefault("Lazer", 0.0)
    val saudeAmt = groupedData.entries.asSequence().filter { it.key.lowercase().contains("saúde") || it.key.lowercase().contains("saude") }.sumOf { it.value }
    val salarioAmt = groupedData.getOrDefault("Salário", 0.0)
    val outrosAmt = groupedData.getOrDefault("Outros", 0.0)

    val totalDistSum = moradiaAmt + alimentacaoAmt + transporteAmt + lazerAmt + saudeAmt + salarioAmt + outrosAmt
    val pcts = if (totalDistSum > 0.0) {
        listOf(
            (moradiaAmt / totalDistSum).toFloat(),
            (alimentacaoAmt / totalDistSum).toFloat(),
            (transporteAmt / totalDistSum).toFloat(),
            (lazerAmt / totalDistSum).toFloat(),
            (saudeAmt / totalDistSum).toFloat(),
            (salarioAmt / totalDistSum).toFloat(),
            (outrosAmt / totalDistSum).toFloat()
        )
    } else {
        listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)
    }
    
    val moradiaPct = pcts[0]
    val alimentacaoPct = pcts[1]
    val transportePct = pcts[2]
    val lazerPct = pcts[3]
    val saudePct = pcts[4]
    val salarioPct = pcts[5]
    val outrosPct = pcts[6]

    val maxPctValue = pcts.maxOrNull() ?: 0f
    val minPctValue = pcts.filter { it > 0 }.minOrNull() ?: 0f

    val moradiaStr = currencyFormat.format(moradiaAmt)
    val alimentacaoStr = currencyFormat.format(alimentacaoAmt)
    val transporteStr = currencyFormat.format(transporteAmt)
    val lazerStr = currencyFormat.format(lazerAmt)
    val saudeStr = currencyFormat.format(saudeAmt)
    val salarioStr = currencyFormat.format(salarioAmt)
    val outrosStr = currencyFormat.format(outrosAmt)

    val chartPoints = remember(allIncomes, allExpenses, selectedPeriod) {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dtfAbbrWeek = java.time.format.DateTimeFormatter.ofPattern("dd/MM")
        val dtfMonth = java.time.format.DateTimeFormatter.ofPattern("MMM", Locale("pt", "PT"))

        when (selectedPeriod) {
            "1 Semana" -> {
                (0..6).map { offset ->
                    val date = today.minusDays((6 - offset).toLong())
                    val dateStr = date.format(formatter)
                    val incSum = allIncomes.asSequence().filter { it.date == dateStr }.sumOf { it.amount }
                    val expSum = allExpenses.asSequence().filter { it.date == dateStr }.sumOf { it.amount }
                    ChartPoint(
                        label = date.format(dtfAbbrWeek),
                        income = incSum,
                        expense = expSum,
                    )
                }
            }
            "1 Mês" -> {
                (0..3).map { offset ->
                    val startDay = today.minusDays((29 - (offset * 7)).toLong())
                    val endDay = today.minusDays((23 - (offset * 7)).toLong())
                    val incSum = allIncomes.asSequence().filter {
                        val d = parseLocalDate(it.date)
                        (d != null) && !d.isBefore(startDay) && !d.isAfter(endDay)
                    }.sumOf { it.amount }
                    val expSum = allExpenses.asSequence().filter {
                        val d = parseLocalDate(it.date)
                        (d != null) && !d.isBefore(startDay) && !d.isAfter(endDay)
                    }.sumOf { it.amount }
                    ChartPoint(
                        label = "Sem ${offset + 1}",
                        income = incSum,
                        expense = expSum,
                    )
                }
            }
            "3 Meses" -> {
                (0..2).map { offset ->
                    val targetMonth = today.minusMonths((2 - offset).toLong())
                    val incSum = allIncomes.filter {
                        val d = parseLocalDate(it.date)
                        d != null && d.year == targetMonth.year && d.month == targetMonth.month
                    }.sumOf { it.amount }
                    val expSum = allExpenses.filter {
                        val d = parseLocalDate(it.date)
                        d != null && d.year == targetMonth.year && d.month == targetMonth.month
                    }.sumOf { it.amount }
                    ChartPoint(
                        label = targetMonth.format(dtfMonth).replace(".", "").replaceFirstChar { it.uppercase() },
                        income = incSum,
                        expense = expSum
                    )
                }
            }
            "6 Meses" -> {
                (0..5).map { offset ->
                    val targetMonth = today.minusMonths((5 - offset).toLong())
                    val incSum = allIncomes.filter {
                        val d = parseLocalDate(it.date)
                        d != null && d.year == targetMonth.year && d.month == targetMonth.month
                    }.sumOf { it.amount }
                    val expSum = allExpenses.filter {
                        val d = parseLocalDate(it.date)
                        d != null && d.year == targetMonth.year && d.month == targetMonth.month
                    }.sumOf { it.amount }
                    ChartPoint(
                        label = targetMonth.format(dtfMonth).replace(".", "").replaceFirstChar { it.uppercase() },
                        income = incSum,
                        expense = expSum
                    )
                }
            }
            "1 Ano" -> {
                (0..11).map { offset ->
                    val targetMonth = today.minusMonths((11 - offset).toLong())
                    val incSum = allIncomes.filter {
                        val d = parseLocalDate(it.date)
                        d != null && d.year == targetMonth.year && d.month == targetMonth.month
                    }.sumOf { it.amount }
                    val expSum = allExpenses.filter {
                        val d = parseLocalDate(it.date)
                        d != null && d.year == targetMonth.year && d.month == targetMonth.month
                    }.sumOf { it.amount }
                    ChartPoint(
                        label = targetMonth.format(dtfMonth).replace(".", "").replaceFirstChar { it.uppercase() },
                        income = incSum,
                        expense = expSum
                    )
                }
            }
            else -> emptyList()
        }
    }

    val maxVal = (chartPoints.map { it.income } + chartPoints.map { it.expense }).maxOrNull()?.coerceAtLeast(10.0) ?: 10.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Relatórios",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Análise de desempenho financeiro",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Botões de Exportação PDF / Excel
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        generateAndSharePDF(
                            context,
                            tokenManager.getUserName(),
                            tokenManager.getUserEmail(),
                            totalIncomes,
                            totalExpenses,
                            moradiaStr,
                            alimentacaoStr,
                            transporteStr,
                            lazerStr,
                            saudeStr,
                            salarioStr,
                            outrosStr
                        )
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E676)),
                    border = BorderStroke(1.dp, Color(0xFF00E676)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportar PDF", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        generateAndShareExcel(
                            context,
                            totalIncomes,
                            totalExpenses,
                            moradiaStr,
                            alimentacaoStr,
                            transporteStr,
                            lazerStr,
                            saudeStr,
                            salarioStr,
                            outrosStr
                        )
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E676)),
                    border = BorderStroke(1.dp, Color(0xFF00E676)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.TableView, contentDescription = "Excel")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportar Excel", fontSize = 12.sp)
                }
            }
        }

        // Cartão de Resumo
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("TOTAL ENTRADAS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(currencyFormat.format(totalIncomes), color = Color(0xFF00E676), fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("TOTAL SAÍDAS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(currencyFormat.format(totalExpenses), color = Color(0xFFFFB4AB), fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFF2C2C2C))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("SALDO LÍQUIDO", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(currencyFormat.format(totalIncomes - totalExpenses), color = Color(0xFF45FEC9), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Pré-visualização do gráfico de fluxo de caixa
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Fluxo de Caixa", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Filtrado por: $selectedPeriod", color = Color.Gray, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        periods.forEach { period ->
                            val isSelected = selectedPeriod == period
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF2A2A2A),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedPeriod = period }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = period,
                                    color = if (isSelected) Color.Black else Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val gridPaintColor = Color(0xFF2A2A2A)
                            for (i in 1..3) {
                                val y = h * (i / 4f)
                                drawLine(gridPaintColor, Offset(0f, y), Offset(w, y), 1.dp.toPx())
                            }

                            val numPoints = chartPoints.size
                            if (numPoints >= 2) {
                                val pathIncomes = Path().apply {
                                    val startY = h - (h * 0.8f * (chartPoints[0].income / maxVal).toFloat())
                                    moveTo(0f, startY)
                                    for (i in 1 until numPoints) {
                                        val x = w * (i.toFloat() / (numPoints - 1))
                                        val y = h - (h * 0.8f * (chartPoints[i].income / maxVal).toFloat())
                                        val prevX = w * ((i - 1).toFloat() / (numPoints - 1))
                                        val prevY = h - (h * 0.8f * (chartPoints[i - 1].income / maxVal).toFloat())
                                        cubicTo(prevX + (x - prevX) / 2f, prevY, prevX + (x - prevX) / 2f, y, x, y)
                                    }
                                }
                                val pathExpenses = Path().apply {
                                    val startY = h - (h * 0.8f * (chartPoints[0].expense / maxVal).toFloat())
                                    moveTo(0f, startY)
                                    for (i in 1 until numPoints) {
                                        val x = w * (i.toFloat() / (numPoints - 1))
                                        val y = h - (h * 0.8f * (chartPoints[i].expense / maxVal).toFloat())
                                        val prevX = w * ((i - 1).toFloat() / (numPoints - 1))
                                        val prevY = h - (h * 0.8f * (chartPoints[i - 1].expense / maxVal).toFloat())
                                        cubicTo(prevX + (x - prevX) / 2f, prevY, prevX + (x - prevX) / 2f, y, x, y)
                                    }
                                }
                                drawPath(path = pathIncomes, color = Color(0xFF00E676), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                                drawPath(path = pathExpenses, color = Color(0xFFFFB4AB), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        chartPoints.forEachIndexed { index, pt ->
                            if (chartPoints.size != 12 || index % 2 == 0) {
                                Text(
                                    text = pt.label, 
                                    color = Color.Gray, 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFF00E676), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Entradas", color = Color.LightGray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(24.dp))
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFB4AB), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Saídas", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }
        }

        // Visualizador de Distribuição de Gastos (Unificado)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Distribuição de Gastos", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(100.dp)) {
                                val strokeWidth = 14.dp.toPx()
                                drawArc(color = Color(0xFF2C2C2C), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(strokeWidth))
                                
                                drawArc(color = Color(0xFF00E676), startAngle = -90f, sweepAngle = 360f * moradiaPct, useCenter = false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                                drawArc(color = Color(0xFF45FEC9), startAngle = -90f + (360f * moradiaPct), sweepAngle = 360f * alimentacaoPct, useCenter = false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                                drawArc(color = Color(0xFFFFDEC4), startAngle = -90f + (360f * (moradiaPct + alimentacaoPct)), sweepAngle = 360f * transportePct, useCenter = false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                                drawArc(color = Color(0xFFFFB1C1), startAngle = -90f + (360f * (moradiaPct + alimentacaoPct + transportePct)), sweepAngle = 360f * lazerPct, useCenter = false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                                drawArc(color = Color(0xFF90CAF9), startAngle = -90f + (360f * (moradiaPct + alimentacaoPct + transportePct + lazerPct)), sweepAngle = 360f * saudePct, useCenter = false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                                drawArc(color = Color(0xFFFFEB3B), startAngle = -90f + (360f * (moradiaPct + alimentacaoPct + transportePct + lazerPct + saudePct)), sweepAngle = 360f * salarioPct, useCenter = false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                                drawArc(color = Color.LightGray, startAngle = -90f + (360f * (moradiaPct + alimentacaoPct + transportePct + lazerPct + saudePct + salarioPct)), sweepAngle = 360f * outrosPct, useCenter = false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("MAIOR", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("${(maxPctValue * 100).toInt()}%", color = Color(0xFF00E676), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            CategoryLegendRow("Moradia", moradiaStr, moradiaPct, Color(0xFF00E676), maxPctValue, minPctValue)
                            CategoryLegendRow("Alimentação", alimentacaoStr, alimentacaoPct, Color(0xFF45FEC9), maxPctValue, minPctValue)
                            CategoryLegendRow("Transporte", transporteStr, transportePct, Color(0xFFFFDEC4), maxPctValue, minPctValue)
                            CategoryLegendRow("Lazer", lazerStr, lazerPct, Color(0xFFFFB1C1), maxPctValue, minPctValue)
                            CategoryLegendRow("Saúde", saudeStr, saudePct, Color(0xFF90CAF9), maxPctValue, minPctValue)
                            CategoryLegendRow("Salário", salarioStr, salarioPct, Color(0xFFFFEB3B), maxPctValue, minPctValue)
                            CategoryLegendRow("Outros", outrosStr, outrosPct, Color.LightGray, maxPctValue, minPctValue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryLegendRow(
    name: String, 
    amount: String, 
    pct: Float, 
    color: Color, 
    maxPct: Float, 
    minPct: Float
) {
    val isMax = pct > 0f && pct == maxPct
    val isMin = pct > 0f && pct == minPct
    
    Row(
        modifier = Modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Text(name, color = Color.LightGray, fontSize = 11.sp)
            if (isMax) {
                Text("(MAIOR)", color = Color(0xFF00E676), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (isMin) {
                Text("(MENOR)", color = Color(0xFFFFB4AB), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${(pct * 100).toInt()}%", color = Color.Gray, fontSize = 11.sp)
            Text(amount, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun generateAndSharePDF(
    context: Context,
    userName: String,
    userEmail: String,
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

    // Header / Logo
    paint.color = android.graphics.Color.parseColor("#00FF66")
    paint.isFakeBoldText = true
    paint.textSize = 40f
    canvas.drawText("$", 50f, 60f, paint)

    paint.color = android.graphics.Color.BLACK
    paint.textSize = 28f
    canvas.drawText("FinFlow Financial Report", 100f, 55f, paint)

    paint.textSize = 14f
    paint.isFakeBoldText = false
    canvas.drawText("Utilizador: $userName", 50f, 100f, paint)
    canvas.drawText("Email: $userEmail", 50f, 120f, paint)
    canvas.drawText("Data de Emissão: ${java.time.LocalDate.now()}", 50f, 140f, paint)

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
        pdfDocument.writeTo(FileOutputStream(file))
        shareFile(context, file, "application/pdf")
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao gerar PDF", Toast.LENGTH_SHORT).show()
    } finally {
        pdfDocument.close()
    }
}

private fun generateAndShareExcel(
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
    csvContent.append("Total Entradas;${incomes}\n")
    csvContent.append("Total Saidas;${expenses}\n")
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
        FileOutputStream(file).use { it.write(csvContent.toString().toByteArray()) }
        shareFile(context, file, "text/csv")
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao gerar Excel/CSV", Toast.LENGTH_SHORT).show()
    }
}

private fun shareFile(context: Context, file: File, mimeType: String) {
    try {
        val authority = "${context.packageName}.fileprovider"
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
