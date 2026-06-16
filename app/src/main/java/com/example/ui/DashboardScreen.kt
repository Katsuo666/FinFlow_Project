package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Expense
import com.example.model.Card
import java.text.NumberFormat
import java.util.Locale

// ---------------- ECRÃ DO SEPARADOR DASHBOARD ----------------
@Composable
fun DashboardScreen(
    balance: Double,
    incomes: Double,
    expenses: Double,
    recentTransactions: List<Expense>,
    onViewAllTransactions: () -> Unit,
    onRefresh: () -> Unit,
    userName: String,
    creditCards: List<Card>,
    onCardClick: (Card) -> Unit,
    onAddCardClick: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "PT")).apply {
        currency = java.util.Currency.getInstance("EUR")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            // Espaçador para o espaçamento do Cabeçalho Superior
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            val firstName = userName.split(" ").firstOrNull() ?: "Utilizador"
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "Bem vindo, $firstName",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Gerencie as suas finanças de forma simples.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }

        // 1. Cartão de Saldo Total
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Saldo Total",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currencyFormat.format(balance),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val netSavings = incomes - expenses
                    val percentValue = if (incomes > 0.0) {
                        (netSavings / incomes) * 100.0
                    } else if (expenses > 0.0) {
                        -100.0
                    } else {
                        0.0
                    }
                    val isPositive = percentValue >= 0.0
                    val absPercent = if (percentValue < 0.0) -percentValue else percentValue
                    val percentString = String.format(Locale.US, "%.1f", absPercent)
                    val percentText = if (isPositive) "+$percentString% este mês" else "-$percentString% este mês"
                    val trendIcon = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown
                    val trendColor = if (isPositive) Color(0xFF00E676) else Color(0xFFFF5252)

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = trendIcon,
                            contentDescription = if (isPositive) "Subiu" else "Desceu",
                            tint = trendColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = percentText,
                            color = trendColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Linha de Receitas & Despesas
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cartão de Receitas
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF003918), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Receitas",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Receitas", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currencyFormat.format(incomes),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Cartão de Despesas
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF690005), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Despesas",
                                tint = Color(0xFFFFB4AB),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Despesas", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currencyFormat.format(expenses),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Cartão(ões) Black
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Meus Cartões",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onAddCardClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar Cartão",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Vincular",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (creditCards.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131313)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF232323))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = "Sem cartões",
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Nenhum cartão associado à sua conta",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onAddCardClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Vincular Novo Cartão", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(creditCards) { card ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onCardClick(card) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF2C2C2C))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF131313))
                    ) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(5.dp)
                                .background(Color(0xFF00E676))
                                .align(Alignment.CenterStart)
                        )

                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = card.name,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (card.type.uppercase() == "CREDIT") "Fatura atual (•••• ${card.lastFourDigits})" else "Conta principal (•••• ${card.lastFourDigits})",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = card.brand,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            val billAmount = card.currentBill ?: 0.0
                            val limitVal = card.limitAmount ?: 0.0
                            val limitDisplay = if (card.type.uppercase() == "CREDIT") limitVal - billAmount else limitVal

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = currencyFormat.format(billAmount),
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (card.type.uppercase() == "CREDIT" && limitVal > 0.0) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Limite disponível", color = Color.Gray, fontSize = 11.sp)
                                    Text(currencyFormat.format(limitDisplay), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                val progress = (billAmount / limitVal).coerceIn(0.0, 1.0).toFloat()
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = progress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = Color(0xFF00E676),
                                    trackColor = Color(0xFF2C2C2C)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Cabeçalho de Atividades Recentes
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Atividades Recentes",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ver todas",
                    color = Color(0xFF00E676),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onViewAllTransactions() }
                )
            }
        }

        // 5. Itens da lista de transações recentes
        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhuma atividade recente",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            items(recentTransactions) { item ->
                TransactionRowItem(item, currencyFormat)
            }
        }
    }
}

@Composable
fun TransactionRowItem(item: Expense, currencyFormat: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (item.isIncome) Color(0xFF003918) else Color(0xFF2C2C2C),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isIncome) Icons.Default.ArrowDownward else Icons.Default.ShoppingCart,
                        contentDescription = "Tipo",
                        tint = if (item.isIncome) Color(0xFF00E676) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = item.description,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = item.date,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = (if (item.isIncome) "+ " else "- ") + currencyFormat.format(item.amount),
                    color = if (item.isIncome) Color(0xFF00E676) else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Box(
                    modifier = Modifier
                        .background(Color(0xFF2C2C2C), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.displayCategoryName,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

fun getStaticSampleTransactions(): List<Expense> {
    return listOf(
        Expense(1, "Supermercado", 450.0, "Hoje, 14:30", 1, "Alimentação", false),
        Expense(2, "Salário", 8500.0, "Ontem, 09:00", 2, "Renda", true),
        Expense(3, "Conta de Luz", 185.50, "12 Fev, 10:15", 3, "Contas", false)
    )
}
