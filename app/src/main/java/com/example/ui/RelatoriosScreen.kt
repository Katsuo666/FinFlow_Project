package com.example.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Expense
import java.text.NumberFormat
import java.util.Locale

// ---------------- ECRÃ DO SEPARADOR RELATÓRIOS ----------------
data class ChartPoint(val label: String, val income: Double, val expense: Double)

@Composable
fun RelatoriosScreen(allIncomes: List<Expense>, allExpenses: List<Expense>) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "PT")).apply {
        currency = java.util.Currency.getInstance("EUR")
    }

    val totalIncomes = allIncomes.sumOf { it.amount }
    val totalExpenses = allExpenses.sumOf { it.amount }

    var selectedPeriod by remember { mutableStateOf("6 Meses") }
    val periods = listOf("1 Semana", "1 Mês", "3 Meses", "6 Meses", "1 Ano")

    val today = try {
        java.time.LocalDate.now()
    } catch (e: Exception) {
        java.time.LocalDate.of(2026, 6, 8)
    }

    val parseLocalDate = { dateStr: String ->
        try {
            java.time.LocalDate.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    val chartPoints = remember(allIncomes, allExpenses, selectedPeriod) {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dtfAbbrWeek = java.time.format.DateTimeFormatter.ofPattern("dd/MM")
        val dtfMonth = java.time.format.DateTimeFormatter.ofPattern("MMM", java.util.Locale("pt", "PT"))

        when (selectedPeriod) {
            "1 Semana" -> {
                (0..6).map { offset ->
                    val date = today.minusDays((6 - offset).toLong())
                    val dateStr = date.format(formatter)
                    val incSum = allIncomes.filter { it.date == dateStr }.sumOf { it.amount }
                    val expSum = allExpenses.filter { it.date == dateStr }.sumOf { it.amount }
                    ChartPoint(
                        label = date.format(dtfAbbrWeek),
                        income = incSum,
                        expense = expSum
                    )
                }
            }
            "1 Mês" -> {
                (0..3).map { offset ->
                    val startDay = today.minusDays((29 - offset * 7).toLong())
                    val endDay = today.minusDays((23 - offset * 7).toLong())
                    val incSum = allIncomes.filter {
                        val d = parseLocalDate(it.date)
                        d != null && !d.isBefore(startDay) && !d.isAfter(endDay)
                    }.sumOf { it.amount }
                    val expSum = allExpenses.filter {
                        val d = parseLocalDate(it.date)
                        d != null && !d.isBefore(startDay) && !d.isAfter(endDay)
                    }.sumOf { it.amount }
                    ChartPoint(
                        label = "Sem ${offset + 1}",
                        income = incSum,
                        expense = expSum
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

    // Métricas dinâmicas para Distribuição baseadas em despesas reais agrupadas por nome da categoria
    var moradiaAmt = 0.0
    var alimentacaoAmt = 0.0
    var transporteAmt = 0.0
    var lazerAmt = 0.0
    var outrosAmt = 0.0

    allExpenses.forEach { tx ->
        val cat = tx.displayCategoryName.lowercase()
        when {
            cat.contains("moradia") || cat.contains("renda") || cat.contains("contas") || cat.contains("luz") || cat.contains("água") || cat.contains("agua") -> {
                moradiaAmt += tx.amount
            }
            cat.contains("alimenta") || cat.contains("restaurante") || cat.contains("supermercado") || cat.contains("comida") -> {
                alimentacaoAmt += tx.amount
            }
            cat.contains("transporte") || cat.contains("carro") || cat.contains("gasolina") -> {
                transporteAmt += tx.amount
            }
            cat.contains("lazer") || cat.contains("viagem") || cat.contains("diversão") || cat.contains("entretenimento") -> {
                lazerAmt += tx.amount
            }
            else -> {
                outrosAmt += tx.amount
            }
        }
    }

    val totalExpSum = moradiaAmt + alimentacaoAmt + transporteAmt + lazerAmt + outrosAmt
    val (moradiaPct, alimentacaoPct, transportePct, lazerPct, outrosPct) = if (totalExpSum > 0.0) {
        listOf(
            (moradiaAmt / totalExpSum).toFloat(),
            (alimentacaoAmt / totalExpSum).toFloat(),
            (transporteAmt / totalExpSum).toFloat(),
            (lazerAmt / totalExpSum).toFloat(),
            (outrosAmt / totalExpSum).toFloat()
        )
    } else {
        listOf(0.20f, 0.20f, 0.20f, 0.20f, 0.20f)
    }

    val maxPct = maxOf(moradiaPct, alimentacaoPct, transportePct, lazerPct, outrosPct)
    val maxPctStr = "${(maxPct * 100).toInt()}%"

    val moradiaStr = currencyFormat.format(moradiaAmt)
    val alimentacaoStr = currencyFormat.format(alimentacaoAmt)
    val transporteStr = currencyFormat.format(transporteAmt)
    val lazerStr = currencyFormat.format(lazerAmt)
    val outrosStr = currencyFormat.format(outrosAmt)

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
                    onClick = {},
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E676)),
                    border = BorderStroke(1.dp, Color(0xFF00E676)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportar PDF", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {},
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

        // Pré-visualização do gráfico de fluxo de caixa (Desenhado usando Canvas dinâmico!)
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

                    // Linha seletora de chips
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

                    // Gráfico de fluxo de caixa estilizado em canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Desenhar linhas de grelha subtis
                            val gridPaintColor = Color(0xFF2A2A2A)
                            for (i in 1..3) {
                                val y = h * (i / 4f)
                                drawLine(gridPaintColor, Offset(0f, y), Offset(w, y), 1.dp.toPx())
                            }

                            val numPoints = chartPoints.size
                            if (numPoints >= 2) {
                                // Curvas dinâmicas baseadas em entradas/saídas reais
                                val pathIncomes = Path().apply {
                                    val startY = h - (h * 0.8f * (chartPoints[0].income / maxVal).toFloat())
                                    moveTo(0f, startY)
                                    for (i in 1 until numPoints) {
                                        val x = w * (i.toFloat() / (numPoints - 1))
                                        val y = h - (h * 0.8f * (chartPoints[i].income / maxVal).toFloat())
                                        val prevX = w * ((i - 1).toFloat() / (numPoints - 1))
                                        val prevY = h - (h * 0.8f * (chartPoints[i - 1].income / maxVal).toFloat())
                                        cubicTo(
                                            prevX + (x - prevX) / 2f, prevY,
                                            prevX + (x - prevX) / 2f, y,
                                            x, y
                                        )
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
                                        cubicTo(
                                            prevX + (x - prevX) / 2f, prevY,
                                            prevX + (x - prevX) / 2f, y,
                                            x, y
                                        )
                                    }
                                }

                                drawPath(
                                    path = pathIncomes,
                                    color = Color(0xFF00E676),
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )

                                drawPath(
                                    path = pathExpenses,
                                    color = Color(0xFFFFB4AB),
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
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

        // Visualizador de Distribuição de Despesas
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
                        // Pequeno Indicador de Progresso em Rosca
                        Box(
                            modifier = Modifier
                                .size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(100.dp)) {
                                val strokeWidth = 14.dp.toPx()
                                drawArc(
                                    color = Color(0xFF2C2C2C),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(strokeWidth)
                                )
                                
                                // Moradia Arc
                                drawArc(
                                    color = Color(0xFF00E676),
                                    startAngle = -90f,
                                    sweepAngle = 360f * moradiaPct,
                                    useCenter = false,
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )

                                // Alimentação Arc
                                drawArc(
                                    color = Color(0xFF45FEC9),
                                    startAngle = -90f + (360f * moradiaPct),
                                    sweepAngle = 360f * alimentacaoPct,
                                    useCenter = false,
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )

                                // Transporte Arc
                                drawArc(
                                    color = Color(0xFFFFDEC4),
                                    startAngle = -90f + (360f * (moradiaPct + alimentacaoPct)),
                                    sweepAngle = 360f * transportePct,
                                    useCenter = false,
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )

                                // Lazer Arc
                                drawArc(
                                    color = Color(0xFFFFB1C1),
                                    startAngle = -90f + (360f * (moradiaPct + alimentacaoPct + transportePct)),
                                    sweepAngle = 360f * lazerPct,
                                    useCenter = false,
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )

                                // Outros Arc
                                drawArc(
                                    color = Color.LightGray,
                                    startAngle = -90f + (360f * (moradiaPct + alimentacaoPct + transportePct + lazerPct)),
                                    sweepAngle = 360f * outrosPct,
                                    useCenter = false,
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("MAIOR", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(maxPctStr, color = Color(0xFF00E676), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Métricas de distribuição
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            CategoryLegendRow("Moradia", moradiaStr, Color(0xFF00E676))
                            CategoryLegendRow("Alimentação", alimentacaoStr, Color(0xFF45FEC9))
                            CategoryLegendRow("Transporte", transporteStr, Color(0xFFFFDEC4))
                            CategoryLegendRow("Lazer", lazerStr, Color(0xFFFFB1C1))
                            CategoryLegendRow("Outros", outrosStr, Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryLegendRow(name: String, amount: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Text(name, color = Color.LightGray, fontSize = 12.sp)
        }
        Text(amount, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
