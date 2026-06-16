package com.example.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.RetrofitClient
import com.example.model.Expense
import com.example.model.Goal
import com.example.model.Card
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.TokenManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

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
    var creditCardsList by remember { mutableStateOf(emptyList<Card>()) }
    var isRefreshing by remember { mutableStateOf(false) }

    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var selectedDetailCard by remember { mutableStateOf<Card?>(null) }

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
                                val payload = Card(
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
