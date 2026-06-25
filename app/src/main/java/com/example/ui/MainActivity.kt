package com.example.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
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
import com.example.ui.dialogs.CreateGoalDialog
import com.example.ui.dialogs.AddCardDialog
import com.example.ui.dialogs.CardDetailsDialog
import com.example.utils.TokenManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager
    private val resumeEpoch = mutableLongStateOf(0L)

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
                        RetrofitClient.reset()
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        intent.putExtra("LOGOUT", true)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    },
                    onOpenAddExpense = {
                        startActivity(Intent(this@MainActivity, AddExpenseActivity::class.java))
                    },
                    resumeEpoch = resumeEpoch.longValue
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeEpoch.longValue = System.currentTimeMillis()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContainer(
    tokenManager: TokenManager,
    onLogout: () -> Unit,
    onOpenAddExpense: () -> Unit,
    resumeEpoch: Long = 0L
) {
    var selectedTab by remember { mutableStateOf("dashboard") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Estados da API
    var balance by remember { mutableDoubleStateOf(0.0) }
    var totalIncomes by remember { mutableStateOf(0.0) }
    var totalExpenses by remember { mutableStateOf(0.0) }
    var rawRecentTransactions by remember { mutableStateOf(emptyList<Expense>()) }
    var allIncomesList by remember { mutableStateOf(emptyList<Expense>()) }
    var allExpensesList by remember { mutableStateOf(emptyList<Expense>()) }
    var goalsList by remember { mutableStateOf(emptyList<Goal>()) }
    var creditCardsList by remember { mutableStateOf(emptyList<Card>()) }
    var isRefreshing by remember { mutableStateOf(value = false) }

    // Estados dos Diálogos
    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var selectedDetailCard by remember { mutableStateOf<Card?>(null) }

    var profileRefreshKey by remember { mutableIntStateOf(0) }

    // Função para obter todos os dados da API
    val fetchAllData = suspend {
        isRefreshing = true
        try {
            val api = RetrofitClient.getApiService(context)

            // Tentar obter dados do perfil para atualizar imagem e bio
            try {
                val profileRes = api.getUserProfile()
                if (profileRes.isSuccessful && profileRes.body() != null) {
                    val p = profileRes.body()!!
                    tokenManager.saveUser(p.name, p.email)
                    if (p.avatarUrl != null) {
                        tokenManager.saveProfilePicture(p.avatarUrl)
                    }
                    // Forçar atualização do ícone na TopBar
                    profileRefreshKey++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Obter receitas e despesas
            val incomesRes = api.getIncomes()
            val expensesRes = api.getExpenses()

            val incomes = incomesRes.body() ?: emptyList()
            val expenses = expensesRes.body() ?: emptyList()

            allIncomesList = incomes
            allExpensesList = expenses

            totalIncomes = incomes.sumOf { it.amount }
            totalExpenses = expenses.sumOf { it.amount }
            balance = totalIncomes - totalExpenses

            // Combinar e ordenar transações recentes
            val combined = (incomes.map { it.copy(isIncome = true) } + 
                           expenses.map { it.copy(isIncome = false) })
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
        topBar = { MainTopBar(tokenManager, { selectedTab = "perfil" }, profileRefreshKey) },
        bottomBar = { MainBottomBar(selectedTab) { selectedTab = it } },
        floatingActionButton = {
            MainFloatingActionButton(
                selectedTab = selectedTab,
                onCreateGoal = { showCreateGoalDialog = true },
                onAddExpense = onOpenAddExpense
            )
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
                    userName = tokenManager.getUserName(),
                    creditCards = creditCardsList,
                    onCardClick = { card -> selectedDetailCard = card },
                    onAddCardClick = { showAddCardDialog = true }
                )
                "despesas" -> ExpenseListContent()
                "metas" -> MetasScreen(
                    goals = goalsList
                )
                "relatorios" -> RelatoriosScreen(
                    allIncomes = allIncomesList,
                    allExpenses = allExpensesList
                )
                "perfil" -> PerfilScreen(
                    tokenManager = tokenManager,
                    onLogout = onLogout,
                    onProfileUpdated = { profileRefreshKey++ }
                )
            }
        }
    }

    // Diálogos Reutilizáveis
    CreateGoalDialog(
        showDialog = showCreateGoalDialog,
        onDismiss = { showCreateGoalDialog = false },
        onGoalCreated = { coroutineScope.launch { fetchAllData() } },
        context = context,
        coroutineScope = coroutineScope
    )

    AddCardDialog(
        showDialog = showAddCardDialog,
        onDismiss = { showAddCardDialog = false },
        onCardAdded = { coroutineScope.launch { fetchAllData() } },
        context = context,
        coroutineScope = coroutineScope
    )

    CardDetailsDialog(
        card = selectedDetailCard,
        onDismiss = { selectedDetailCard = null },
        onCardDeleted = { coroutineScope.launch { fetchAllData() } },
        context = context,
        coroutineScope = coroutineScope,
        onRemoveCard = { card -> creditCardsList = creditCardsList.filter { it.id != card.id } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(tokenManager: TokenManager, onProfileClick: () -> Unit, refreshKey: Int) {
    val profilePicUri = remember(refreshKey) { tokenManager.getProfilePicture() }
    
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
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePicUri != null) {
                        coil.compose.AsyncImage(
                            model = profilePicUri,
                            contentDescription = "Foto de Perfil",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Utilizador",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "FinFlow",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
    )
}

@Composable
private fun MainBottomBar(selectedTab: String, onTabSelected: (String) -> Unit) {
    NavigationBar(containerColor = Color(0xFF131313), tonalElevation = 8.dp) {
        val items = listOf(
            "dashboard" to Pair(Icons.Default.Dashboard, "Dashboard"),
            "despesas" to Pair(Icons.Default.CreditCard, "Despesas"),
            "metas" to Pair(Icons.Default.Flag, "Metas"),
            "relatorios" to Pair(Icons.Default.BarChart, "Relatórios")
        )

        items.forEach { (tabId, iconLabel) ->
            NavigationBarItem(
                selected = selectedTab == tabId,
                onClick = { onTabSelected(tabId) },
                icon = { Icon(iconLabel.first, contentDescription = iconLabel.second) },
                label = { Text(iconLabel.second) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF131313),
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.LightGray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}

@Composable
private fun MainFloatingActionButton(
    selectedTab: String,
    onCreateGoal: () -> Unit,
    onAddExpense: () -> Unit
) {
    when (selectedTab) {
        "metas" -> FloatingActionButton(
            onClick = onCreateGoal,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nova Meta", modifier = Modifier.size(28.dp))
        }
        "despesas" -> FloatingActionButton(
            onClick = onAddExpense,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nova Transação", modifier = Modifier.size(28.dp))
        }
    }
}
