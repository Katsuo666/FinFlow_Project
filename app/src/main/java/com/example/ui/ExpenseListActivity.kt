package com.example.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.RetrofitClient
import com.example.model.Expense
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.TokenManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ExpenseListActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager
    private val resumeEpoch = mutableStateOf(0L)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        tokenManager = TokenManager(this)

        if (tokenManager.getToken() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            MyApplicationTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("Suas Despesas", color = Color.White, fontWeight = FontWeight.Bold)
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFF131313),
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = false,
                                onClick = {
                                    val intent = Intent(this@ExpenseListActivity, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                    startActivity(intent)
                                    finish()
                                },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Dashboard") },
                                colors = NavigationBarItemDefaults.colors(
                                    unselectedIconColor = Color.LightGray,
                                    unselectedTextColor = Color.Gray
                                )
                            )
                            NavigationBarItem(
                                selected = true,
                                onClick = { },
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
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                startActivity(Intent(this@ExpenseListActivity, AddExpenseActivity::class.java))
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Nova Transação", modifier = Modifier.size(28.dp))
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF121212))
                            .padding(innerPadding)
                    ) {
                        ExpenseListContent(resumeEpoch = resumeEpoch.value)
                    }
                }
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
fun ExpenseListContent(resumeEpoch: Long = 0L) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "PT")).apply {
        currency = java.util.Currency.getInstance("EUR")
    }

    var activeFilter by remember { mutableStateOf("Tudo") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    // Listas de estado da API
    var transactions by remember { mutableStateOf(emptyList<Expense>()) }
    var isLoading by remember { mutableStateOf(true) }

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

    val loadTransactions = suspend {
        isLoading = true
        try {
            val api = RetrofitClient.getApiService(context)
            val responseExpenses = api.getExpenses()
            val responseIncomes = api.getIncomes()

            val exps = responseExpenses.body() ?: emptyList()
            val incs = responseIncomes.body() ?: emptyList()

            // Mapear e combinar
            transactions = (exps.map { it.copy(isIncome = false) } + incs.map { it.copy(isIncome = true) })
                .sortedByDescending { it.date }
        } catch (e: Exception) {
            e.printStackTrace()
            transactions = emptyList()
            Toast.makeText(context, "Erro de ligação ao servidor", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(resumeEpoch) {
        loadTransactions()
    }

    // Cálculo de filtros
    val filteredTransactions = remember(transactions, searchQuery, activeFilter) {
        transactions.filter { tx ->
            val matchQuery = searchQuery.isEmpty() || tx.description.contains(searchQuery, ignoreCase = true) || tx.displayCategoryName.contains(searchQuery, ignoreCase = true)
            val matchFilter = when (activeFilter) {
                "1 Semana" -> {
                    val limitDate = today.minusDays(6)
                    val d = parseLocalDate(tx.date)
                    d != null && !d.isBefore(limitDate) && !d.isAfter(today)
                }
                "1 Mês" -> {
                    val limitDate = today.minusDays(29)
                    val d = parseLocalDate(tx.date)
                    d != null && !d.isBefore(limitDate) && !d.isAfter(today)
                }
                "3 Meses" -> {
                    val limitDate = today.minusMonths(3)
                    val d = parseLocalDate(tx.date)
                    d != null && !d.isBefore(limitDate) && !d.isAfter(today)
                }
                "6 Meses" -> {
                    val limitDate = today.minusMonths(6)
                    val d = parseLocalDate(tx.date)
                    d != null && !d.isBefore(limitDate) && !d.isAfter(today)
                }
                "1 Ano" -> {
                    val limitDate = today.minusYears(1)
                    val d = parseLocalDate(tx.date)
                    d != null && !d.isBefore(limitDate) && !d.isAfter(today)
                }
                else -> true
            }
            matchQuery && matchFilter
        }
    }

    val totalExpensesSum = remember(filteredTransactions) {
        filteredTransactions.filter { !it.isIncome }.sumOf { it.amount }
    }

    val maxExpenseCategory = remember(filteredTransactions) {
        val expenseTx = filteredTransactions.filter { !it.isIncome }
        if (expenseTx.isEmpty()) "Nenhuma"
        else {
            expenseTx.groupBy { it.displayCategoryName }
                .maxByOrNull { entry -> entry.value.sumOf { it.amount } }?.key ?: "Outros"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Linha de chips de filtro e barra de pesquisa
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!showSearch) {
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Tudo", "1 Semana", "1 Mês", "3 Meses", "6 Meses", "1 Ano").forEach { filter ->
                        val isSelected = activeFilter == filter
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1E1E1E),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { activeFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) Color(0xFF131313) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { showSearch = true }) {
                    Icon(Icons.Default.Search, contentDescription = "Pesquisar", tint = Color.LightGray)
                }
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Pesquisar...", color = Color.Gray, fontSize = 12.sp) },
                    trailingIcon = {
                        IconButton(onClick = {
                            searchQuery = ""
                            showSearch = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar Pesquisa", tint = Color.Gray)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF2C2C2C),
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Cartões de Resumo Agregados (Total Gasto e Maior Categoria)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Caixa de Total Gasto
            Card(
                modifier = Modifier.weight(1.5f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TOTAL GASTO", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "- " + currencyFormat.format(totalExpensesSum),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Caixa de Maior Categoria
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("MAIOR CATEGORIA", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = maxExpenseCategory,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = maxExpenseCategory,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Lista de transações ordenada e estruturada
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                val grouped = filteredTransactions.groupBy {
                    it.date.substringBefore(",") // Categorizador de dia simplista, ex: "Hoje" / "Ontem"
                }

                if (grouped.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Nenhuma transação encontrada", color = Color.Gray)
                        }
                    }
                } else {
                    grouped.forEach { (header, listItems) ->
                        item {
                            Text(
                                text = header.uppercase(),
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(listItems) { item ->
                            TransactionRowItem(item, currencyFormat)
                        }
                    }
                }
            }
        }
    }
}

fun getStaticFullTransactions(): List<Expense> {
    return listOf(
        Expense(1, "Restaurante Sabor", 85.50, "Hoje, 12:30", 1, "Alimentação", false),
        Expense(2, "Uber", 32.00, "Hoje, 09:15", 2, "Transporte", false),
        Expense(3, "Supermercado Extra", 450.20, "Ontem, 18:45", 1, "Alimentação", false),
        Expense(4, "Conta de Luz", 180.00, "Ontem, 10:00", 3, "Moradia", false)
    )
}
