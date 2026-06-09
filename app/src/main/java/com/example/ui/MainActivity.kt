package com.example.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.api.RetrofitClient
import com.example.model.Expense
import com.example.model.Goal
import com.example.model.CreditCard
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.TokenManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip

class MainActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager
    private val resumeEpoch = mutableStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        tokenManager = TokenManager(this)

        // Verificação de Segurança
        if (tokenManager.getToken() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            MyApplicationTheme {
                MainScreenContainer(
                    tokenManager = tokenManager,
                    onLogout = {
                        tokenManager.clear()
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    },
                    onOpenAddExpense = {
                        startActivity(Intent(this@MainActivity, AddExpenseActivity::class.java))
                    },
                    onOpenExpensesList = {
                        val intent = Intent(this@MainActivity, ExpenseListActivity::class.java)
                        startActivity(intent)
                    },
                    resumeEpoch = resumeEpoch.value
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeEpoch.value = System.currentTimeMillis()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContainer(
    tokenManager: TokenManager,
    onLogout: () -> Unit,
    onOpenAddExpense: () -> Unit,
    onOpenExpensesList: () -> Unit,
    resumeEpoch: Long = 0L
) {
    var selectedTab by remember { mutableStateOf("dashboard") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Estados da API
    var balance by remember { mutableStateOf(0.0) }
    var totalIncomes by remember { mutableStateOf(0.0) }
    var totalExpenses by remember { mutableStateOf(0.0) }
    var rawRecentTransactions by remember { mutableStateOf(emptyList<Expense>()) }
    var allIncomesList by remember { mutableStateOf(emptyList<Expense>()) }
    var allExpensesList by remember { mutableStateOf(emptyList<Expense>()) }
    var goalsList by remember { mutableStateOf(emptyList<Goal>()) }
    var creditCardsList by remember { mutableStateOf(emptyList<CreditCard>()) }
    var isRefreshing by remember { mutableStateOf(false) }

    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var selectedDetailCard by remember { mutableStateOf<CreditCard?>(null) }

    // Obter Dados da API
    val fetchAllData = suspend {
        isRefreshing = true
        try {
            val api = RetrofitClient.getApiService(context)

            // Obtenção direta de listas de fontes individuais para garantir sincronização 100% em tempo real
            val incomesRes = api.getIncomes()
            val expensesRes = api.getExpenses()

            val incs = incomesRes.body() ?: emptyList()
            val exps = expensesRes.body() ?: emptyList()

            allIncomesList = incs
            allExpensesList = exps

            totalIncomes = incs.sumOf { it.amount }
            totalExpenses = exps.sumOf { it.amount }
            balance = totalIncomes - totalExpenses

            // Combinar lista para atividades recentes (obter as últimas 6)
            val combined = (incs.map { it.copy(isIncome = true) } + exps.map { it.copy(isIncome = false) })
                .sortedByDescending { it.date }
            rawRecentTransactions = combined.take(6)

            // Carregar metas
            val goalsRes = api.getGoals()
            if (goalsRes.isSuccessful && goalsRes.body() != null) {
                goalsList = goalsRes.body()!!
            }

            // Carregar cartões de crédito
            try {
                val cardsRes = api.getCreditCards()
                if (cardsRes.isSuccessful && cardsRes.body() != null) {
                    creditCardsList = cardsRes.body()!!
                }
            } catch (cardEx: Exception) {
                cardEx.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isRefreshing = false
        }
    }

    LaunchedEffect(resumeEpoch) {
        fetchAllData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF2C2C2C), shape = CircleShape)
                                .clickable { selectedTab = "perfil" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Utilizador",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "FinFlow",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF131313),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == "dashboard",
                    onClick = { selectedTab = "dashboard" },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF131313),
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == "despesas",
                    onClick = { selectedTab = "despesas" },
                    icon = { Icon(Icons.Default.CreditCard, contentDescription = "Despesas") },
                    label = { Text("Despesas") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF131313),
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == "metas",
                    onClick = { selectedTab = "metas" },
                    icon = { Icon(Icons.Default.Flag, contentDescription = "Metas") },
                    label = { Text("Metas") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF131313),
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == "relatorios",
                    onClick = { selectedTab = "relatorios" },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Relatórios") },
                    label = { Text("Relatórios") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF131313),
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == "metas") {
                FloatingActionButton(
                    onClick = { showCreateGoalDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nova Meta",
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else if (selectedTab == "despesas") {
                FloatingActionButton(
                    onClick = onOpenAddExpense,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nova Transação",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                "dashboard" -> DashboardScreen(
                    balance = balance,
                    incomes = totalIncomes,
                    expenses = totalExpenses,
                    recentTransactions = rawRecentTransactions,
                    onViewAllTransactions = { selectedTab = "despesas" },
                    onRefresh = { coroutineScope.launch { fetchAllData() } },
                    userName = tokenManager.getUserName(),
                    creditCards = creditCardsList,
                    onCardClick = { card -> selectedDetailCard = card },
                    onAddCardClick = { showAddCardDialog = true }
                )
                "despesas" -> ExpenseListContent()
                "metas" -> MetasScreen(
                    goals = goalsList,
                    onRefresh = { coroutineScope.launch { fetchAllData() } }
                )
                "relatorios" -> RelatoriosScreen(
                    allIncomes = allIncomesList,
                    allExpenses = allExpensesList
                )
                "perfil" -> PerfilScreen(
                    tokenManager = tokenManager,
                    onLogout = onLogout
                )
            }
        }
    }

    // ---------------- SOBREPOSIÇÕES DE DIÁLOGO DE TAREFAS ----------------

    // 1. DIÁLOGO DE CRIAÇÃO DE META
    if (showCreateGoalDialog) {
        var goalName by remember { mutableStateOf("") }
        var targetAmtStr by remember { mutableStateOf("") }
        var currentAmtStr by remember { mutableStateOf("") }
        var deadlineStr by remember { mutableStateOf("") }
        var isSavingGoal by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCreateGoalDialog = false },
            title = { Text("Criar Nova Meta", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = Color(0xFF1E1E1E),
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = goalName,
                        onValueChange = { goalName = it },
                        label = { Text("Nome da Meta") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.Gray,
                            unfocusedBorderColor = Color(0xFF2C2C2C),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetAmtStr,
                        onValueChange = { targetAmtStr = it },
                        label = { Text("Valor Alvo (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.Gray,
                            unfocusedBorderColor = Color(0xFF2C2C2C),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = currentAmtStr,
                        onValueChange = { currentAmtStr = it },
                        label = { Text("Valor Guardado Atual (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.Gray,
                            unfocusedBorderColor = Color(0xFF2C2C2C),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = deadlineStr,
                        onValueChange = { deadlineStr = it },
                        label = { Text("Prazo (Ex: 2026-12-31)") },
                        placeholder = { Text("yyyy-MM-dd", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.Gray,
                            unfocusedBorderColor = Color(0xFF2C2C2C),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetVal = targetAmtStr.toDoubleOrNull() ?: 0.0
                        val currentVal = currentAmtStr.toDoubleOrNull() ?: 0.0
                        if (goalName.isEmpty()) {
                            Toast.makeText(context, "Por favor insira um nome.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (targetVal <= 0.0) {
                            Toast.makeText(context, "Por favor insira um valor alvo válido.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSavingGoal = true
                        coroutineScope.launch {
                            try {
                                val api = RetrofitClient.getApiService(context)
                                val deadlineFormatted = if (deadlineStr.isEmpty()) "2026-12-31" else deadlineStr
                                val payload = Goal(
                                    id = null,
                                    name = goalName,
                                    targetAmount = targetVal,
                                    currentAmount = currentVal,
                                    deadline = deadlineFormatted
                                )
                                val res = api.createGoal(payload)
                                if (res.isSuccessful) {
                                    Toast.makeText(context, "Meta criada com sucesso!", Toast.LENGTH_SHORT).show()
                                    showCreateGoalDialog = false
                                    fetchAllData()
                                } else {
                                    Toast.makeText(context, "Por favor tente novamente.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Sem ligação à internet. Tentando offline.", Toast.LENGTH_SHORT).show()
                                showCreateGoalDialog = false
                            } finally {
                                isSavingGoal = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !isSavingGoal
                ) {
                    if (isSavingGoal) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Criar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGoalDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    // 2. DIÁLOGO DE VINCULAÇÃO DE NOVO CARTÃO DE CRÉDITO
    if (showAddCardDialog) {
        var cardName by remember { mutableStateOf("") }
        var cardholderName by remember { mutableStateOf("") }
        var digits by remember { mutableStateOf("") }
        var expiry by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("CREDIT") } // CRÉDITO ou DÉBITO
        var limitAmtStr by remember { mutableStateOf("") }
        var currentBillStr by remember { mutableStateOf("") }
        var brand by remember { mutableStateOf("VISA") } // VISA ou MASTERCARD
        var isSavingCard by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddCardDialog = false },
            title = { Text("Vincular Novo Cartão", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = Color(0xFF1E1E1E),
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = cardName,
                            onValueChange = { cardName = it },
                            label = { Text("Nome do Cartão (Ex: Millennium)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF2C2C2C),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = cardholderName,
                            onValueChange = { cardholderName = it },
                            label = { Text("Nome do Titular (Ex: MARIA SILVA)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF2C2C2C),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = digits,
                                onValueChange = { if (it.length <= 4) digits = it },
                                label = { Text("Últimos 4 Dígitos") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF2C2C2C),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = expiry,
                                onValueChange = { expiry = it },
                                label = { Text("Validade (yyyy-MM-dd)") },
                                placeholder = { Text("Ex: 2030-05-01") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF2C2C2C),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Text("Tipo de Cartão", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { type = "CREDIT" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (type == "CREDIT") MaterialTheme.colorScheme.primary else Color(0xFF2C2C2C)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Crédito", color = if (type == "CREDIT") Color.Black else Color.White, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { type = "DEBIT" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (type == "DEBIT") MaterialTheme.colorScheme.primary else Color(0xFF2C2C2C)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Débito", color = if (type == "DEBIT") Color.Black else Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                    item {
                        Text("Bandeira", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { brand = "VISA" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (brand == "VISA") MaterialTheme.colorScheme.primary else Color(0xFF2C2C2C)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Visa", color = if (brand == "VISA") Color.Black else Color.White, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { brand = "MASTERCARD" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (brand == "MASTERCARD") MaterialTheme.colorScheme.primary else Color(0xFF2C2C2C)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Mastercard", color = if (brand == "MASTERCARD") Color.Black else Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                    if (type == "CREDIT") {
                        item {
                            OutlinedTextField(
                                value = limitAmtStr,
                                onValueChange = { limitAmtStr = it },
                                label = { Text("Limite Total (€)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF2C2C2C),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = currentBillStr,
                                onValueChange = { currentBillStr = it },
                                label = { Text("Fatura Atual (€)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF2C2C2C),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limitVal = limitAmtStr.toDoubleOrNull() ?: 2000.0
                        val billVal = currentBillStr.toDoubleOrNull() ?: 0.0
                        if (cardName.isEmpty() || cardholderName.isEmpty()) {
                            Toast.makeText(context, "Por favor insira os dados do cartão.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (digits.length < 4) {
                            Toast.makeText(context, "Por favor insira os 4 últimos dígitos.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSavingCard = true
                        coroutineScope.launch {
                            try {
                                val api = RetrofitClient.getApiService(context)
                                val finalExpiry = if (expiry.isEmpty()) "2031-01-01" else expiry
                                val payload = CreditCard(
                                    id = null,
                                    name = cardName,
                                    cardholderName = cardholderName.uppercase(),
                                    lastFourDigits = digits,
                                    expirationDate = finalExpiry,
                                    type = type,
                                    limitAmount = limitVal,
                                    currentBill = if (type == "CREDIT") billVal else 0.0,
                                    brand = brand
                                )
                                val res = api.createCreditCard(payload)
                                if (res.isSuccessful) {
                                    Toast.makeText(context, "Cartão vinculado com sucesso!", Toast.LENGTH_SHORT).show()
                                    showAddCardDialog = false
                                    fetchAllData()
                                } else {
                                    Toast.makeText(context, "Erro do servidor.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Erro de ligação. Cartão simulado offline.", Toast.LENGTH_SHORT).show()
                                showAddCardDialog = false
                            } finally {
                                isSavingCard = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !isSavingCard
                ) {
                    if (isSavingCard) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Vincular", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCardDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    // 3. DIÁLOGO DE DETALHES E DESVINCULAÇÃO DE CARTÃO DE CRÉDITO
    selectedDetailCard?.let { card ->
        var isDeletingCard by remember { mutableStateOf(false) }
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "PT")).apply {
            currency = java.util.Currency.getInstance("EUR")
        }

        AlertDialog(
            onDismissRequest = { selectedDetailCard = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(card.name, color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Titular: ${card.cardholderName}", color = Color.White, fontSize = 14.sp)
                    Text("Número: •••• •••• •••• ${card.lastFourDigits}", color = Color.LightGray, fontSize = 14.sp)
                    Text("Validade: ${card.expirationDate}", color = Color.LightGray, fontSize = 14.sp)
                    Text("Bandeira: ${card.brand}", color = Color.LightGray, fontSize = 14.sp)
                    Text("Tipo: ${card.type}", color = Color.LightGray, fontSize = 14.sp)

                    Divider(color = Color(0xFF2C2C2C), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                    if (card.type.uppercase() == "CREDIT") {
                        val limit = card.limitAmount ?: 0.0
                        val bill = card.currentBill ?: 0.0
                        Text("Fatura Atual: ${currencyFormat.format(bill)}", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Limite Total: ${currencyFormat.format(limit)}", color = Color.LightGray)
                        Text("Disponível: ${currencyFormat.format(limit - bill)}", color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                    } else {
                        val balanceVal = card.limitAmount ?: 0.0
                        Text("Saldo da Conta: ${currencyFormat.format(balanceVal)}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cardId = card.id
                        if (cardId != null) {
                            isDeletingCard = true
                            coroutineScope.launch {
                                try {
                                    val api = RetrofitClient.getApiService(context)
                                    val response = api.deleteCreditCard(cardId)
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "Cartão desvinculado com sucesso!", Toast.LENGTH_SHORT).show()
                                        selectedDetailCard = null
                                        fetchAllData()
                                    } else {
                                        Toast.makeText(context, "Erro ao desvincular.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    // Remover da listagem offline diretamente
                                    creditCardsList = creditCardsList.filter { it.id != cardId }
                                    selectedDetailCard = null
                                    Toast.makeText(context, "Desvinculado localmente.", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isDeletingCard = false
                                }
                            }
                        } else {
                            // Desseleção de cartão offline/simulado
                            creditCardsList = creditCardsList.filter { it.lastFourDigits != card.lastFourDigits }
                            selectedDetailCard = null
                            Toast.makeText(context, "Desvinculado com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF690005)),
                    enabled = !isDeletingCard
                ) {
                    if (isDeletingCard) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Desvincular Cartão", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDetailCard = null }) {
                    Text("Voltar", color = Color.Gray)
                }
            }
        )
    }
}

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
    creditCards: List<CreditCard>,
    onCardClick: (CreditCard) -> Unit,
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

// ---------------- ECRÃ DO SEPARADOR METAS ----------------
@Composable
fun MetasScreen(goals: List<Goal>, onRefresh: () -> Unit) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "PT")).apply {
        currency = java.util.Currency.getInstance("EUR")
    }

    val displayGoals = goals

    val totalCurrent = goals.sumOf { it.currentAmount }
    val totalTarget = goals.sumOf { it.targetAmount }
    val progressPercent = if (totalTarget > 0) ((totalCurrent / totalTarget) * 100).toInt() else 0
    val activeCount = goals.count { it.currentAmount < it.targetAmount }
    val completedCount = goals.count { it.currentAmount >= it.targetAmount }

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
                text = "Metas",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Acompanhe seus objetivos",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Cartão do Total Guardado
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("TOTAL GUARDADO", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        currencyFormat.format(totalCurrent),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("De ${currencyFormat.format(totalTarget)} ($progressPercent%)", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }

        // Caixas de estatísticas
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Flag, contentDescription = "Ativas", tint = Color(0xFF00E676))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("$activeCount", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Metas Ativas", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Concluídas", tint = Color(0xFF45FEC9))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("$completedCount", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Concluída", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }

        // Detalhes das metas
        if (displayGoals.isEmpty()) {
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
                            text = "Nenhuma meta cadastrada",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            items(displayGoals) { goal ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF2C2C2C), shape = RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = "Goal", tint = Color(0xFF00E676))
                                }
                                Column {
                                    Text(goal.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF003918), shape = RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Lazer", color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        if (!goal.deadline.isNullOrEmpty()) {
                                            Text(goal.deadline, color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${currencyFormat.format(goal.currentAmount)} / ${currencyFormat.format(goal.targetAmount)}",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${(progress * 100).toInt()}%",
                                color = Color(0xFF00E676),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = Color(0xFF00E676),
                            trackColor = Color(0xFF2C2C2C)
                        )
                    }
                }
            }
        }
    }
}

fun getStaticSampleGoals(): List<Goal> {
    return listOf(
        Goal(1, "Viagem Japão", 15000.0, 8500.0, "Dez 2024"),
        Goal(2, "Reserva de Emergência", 10000.0, 5000.0, "Nov 2024"),
        Goal(3, "Novo Setup", 5000.0, 1000.0, "Ago 2024")
    )
}

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

@Composable
fun PerfilScreen(
    tokenManager: TokenManager,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var profilePicUri by remember { mutableStateOf(tokenManager.getProfilePicture()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            profilePicUri = uri.toString()
            tokenManager.saveProfilePicture(uri.toString())
            Toast.makeText(context, "Foto de perfil atualizada!", Toast.LENGTH_SHORT).show()
        }
    }

    val profileBitmap = remember(profilePicUri) {
        if (profilePicUri != null) {
            try {
                val contentResolver = context.contentResolver
                val uri = android.net.Uri.parse(profilePicUri)
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    val source = android.graphics.ImageDecoder.createSource(contentResolver, uri)
                    android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }.asImageBitmap()
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri).asImageBitmap()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirm = false },
            title = { Text("Apagar a sua conta?", color = Color.White) },
            text = { Text("Esta ação é completamente irreversível. Todos os seus dados de despesas, receitas e cartões serão excluídos permanentemente.", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        coroutineScope.launch {
                            try {
                                val api = RetrofitClient.getApiService(context)
                                val response = api.deleteAccount()
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "A sua conta foi permanentemente apagada.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Conta eliminada localmente com sucesso.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Servidor indisponível. Conta eliminada localmente.", Toast.LENGTH_LONG).show()
                            } finally {
                                isDeleting = false
                                showDeleteConfirm = false
                                tokenManager.saveProfilePicture(null)
                                onLogout()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Apagar Permanentemente")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancelar", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Acento visual do avatar com clique para carregar e indicador de edição
        Box(
            modifier = Modifier
                .size(110.dp)
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFF1E1E1E), shape = CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (profileBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = profileBitmap,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Mudar foto",
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Títulos de Nome e E-mail
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = tokenManager.getUserName(),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = tokenManager.getUserEmail(),
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cartão de itens de conteúdo do perfil
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileDetailRow(label = "Aplicação", value = "FinFlow Mobile")
                HorizontalDivider(color = Color(0xFF2C2C2C))
                ProfileDetailRow(label = "Idioma", value = "Português (Portugal)")
                HorizontalDivider(color = Color(0xFF2C2C2C))
                ProfileDetailRow(label = "Tipo de Conta", value = "Utilizador Standard")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botão "Apagar Conta" com contorno vermelho
        OutlinedButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFFFB4AB)
            ),
            border = BorderStroke(1.5.dp, Color(0xFFBA1A1A)),
            shape = RoundedCornerShape(26.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = Color(0xFFFFB4AB)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Apagar Conta",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Botão de Sair personalizado
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3A3A3C),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sair da Conta",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
