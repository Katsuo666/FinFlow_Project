package com.example.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.RetrofitClient
import com.example.model.Category
import com.example.model.Expense
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.TokenManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager

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
                Scaffold { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF121212))
                            .padding(innerPadding)
                    ) {
                        AddExpenseScreenContent(onSuccess = {
                            Toast.makeText(this@AddExpenseActivity, "Transação guardada com sucesso!", Toast.LENGTH_SHORT).show()
                            finish()
                        }, onCancel = {
                            finish()
                        })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreenContent(onSuccess: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Atributos do formulário
    var amountStr by remember { mutableStateOf("0.00") }
    var description by remember { mutableStateOf("") }
    var rawDateString by remember { mutableStateOf("") } // dd/MM/yyyy mostrado ao utilizador
    var isIncome by remember { mutableStateOf(false) } // Alternar entre Receita & Despesa
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var isLoadingCategories by remember { mutableStateOf(false) }
    var hasCategoryLoadError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var systemCategories by remember { mutableStateOf(listOf<Category>()) }
    var isUsingLocalFallbackCategories by remember { mutableStateOf(false) }

    val loadCategoriesFromApi = suspend {
        isLoadingCategories = true
        hasCategoryLoadError = false
        val defaultCategories = listOf(
            Category(1, "Vencimento"),
            Category(2, "Moradia"),
            Category(3, "Alimentação"),
            Category(4, "Transporte"),
            Category(5, "Lazer"),
            Category(6, "Outros")
        )
        try {
            val api = RetrofitClient.getApiService(context)
            val res = api.getCategories()
            if (res.isSuccessful && res.body() != null) {
                val apiCats = res.body()!!
                if (apiCats.isEmpty()) {
                    systemCategories = defaultCategories
                    selectedCategory = defaultCategories.firstOrNull()
                    isUsingLocalFallbackCategories = true
                } else {
                    val finalCategories = apiCats.distinctBy { it.id }
                    systemCategories = finalCategories
                    selectedCategory = finalCategories.firstOrNull()
                    isUsingLocalFallbackCategories = false
                }
            } else {
                systemCategories = defaultCategories
                selectedCategory = defaultCategories.firstOrNull()
                isUsingLocalFallbackCategories = true
                hasCategoryLoadError = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            systemCategories = defaultCategories
            selectedCategory = defaultCategories.firstOrNull()
            isUsingLocalFallbackCategories = true
            hasCategoryLoadError = true
        } finally {
            isLoadingCategories = false
        }
    }

    // No Início, carregar categorias e pré-preencher a data
    LaunchedEffect(Unit) {
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        rawDateString = currentDate
        loadCategoriesFromApi()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Linha do Cabeçalho de Título Superior
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = Color.LightGray)
            }
            Text(
                text = if (isIncome) "Nova Receita" else "Nova Despesa",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp)) // Espaçador equalizador
        }

        // Alternador (Toggle) para Receita vs Despesa
        Row(
            modifier = Modifier
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        if (!isIncome) Color(0xFF690005) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { isIncome = false }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Despesa (Saída)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .background(
                        if (isIncome) Color(0xFF003918) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { isIncome = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Receita (Entrada)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 2. PAINEL DE ENTRADA DE VALOR
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("VALOR", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "€",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 6.dp)
                )
                BasicTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .width(220.dp)
                        .border(BorderStroke(1.dp, Color(0xFF2C2C2C)), RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // 3. Campo de Descrição
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Descrição", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("O que você comprou?", color = Color.DarkGray) },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = "Descrição", tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFF2C2C2C),
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // 4. Seletores horizontais dinâmicos de chips de categorias
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Categoria", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (isLoadingCategories) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    Text("A carregar as categorias...", color = Color.Gray, fontSize = 13.sp)
                }
            } else if (hasCategoryLoadError || systemCategories.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Deu erro a obter categorias da API", color = Color(0xFFFF5252), fontSize = 13.sp)
                    TextButton(onClick = {
                        coroutineScope.launch {
                            loadCategoriesFromApi()
                        }
                    }) {
                        Text("Tentar Novamente", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(systemCategories) { category ->
                        val isSelected = selectedCategory?.id == category.id
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Color(0xFF003918) else Color(0xFF1C1C1E),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = when (category.displayName.lowercase()) {
                                        "salário", "salário / vencimento", "vencimento" -> Icons.Default.MonetizationOn
                                        "moradia", "moradia (renda/luz/água)", "renda" -> Icons.Default.Home
                                        "alimentação", "restaurante", "supermercado" -> Icons.Default.Restaurant
                                        "transporte", "carro", "gasolina" -> Icons.Default.DirectionsCar
                                        "lazer" -> Icons.Default.Celebration
                                        else -> Icons.Default.Tag
                                    },
                                    contentDescription = category.displayName,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = category.displayName,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Seletor de Data de Calendário
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Data", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = rawDateString,
                onValueChange = { rawDateString = it },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Data", tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFF2C2C2C),
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.weight(1f)) // Empurrador de elemento inferior

        // 6. BOTÃO DE GATILHO PARA GUARDAR TRANSAÇÃO (Página 7 "Salvar Despesa")
        Button(
            onClick = {
                val amtValue = amountStr.toDoubleOrNull() ?: 0.0
                if (amtValue <= 0) {
                    Toast.makeText(context, "Por favor insira um valor válido.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (description.isEmpty()) {
                    Toast.makeText(context, "Por favor insira uma descrição.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (selectedCategory == null) {
                    Toast.makeText(context, "Por favor selecione uma categoria válida.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val apiDate = formatToApiDate(rawDateString)

                isSaving = true
                coroutineScope.launch {
                    try {
                        val api = RetrofitClient.getApiService(context)
                        val payloadItem = Expense(
                            id = null,
                            description = description,
                            amount = amtValue,
                            date = apiDate,
                            categoryId = selectedCategory?.id,
                            categoryName = selectedCategory?.displayName,
                            category = null // Enviar null para evitar erro de desserialização no servidor
                        )

                        val response = if (isIncome) {
                            api.createIncome(payloadItem)
                        } else {
                            api.createExpense(payloadItem)
                        }

                        if (response.isSuccessful) {
                            onSuccess()
                        } else {
                            val errorStr = response.errorBody()?.string() ?: ""
                            val userFriendlyMessage = try {
                                val json = org.json.JSONObject(errorStr)
                                json.optString("message", "Erro desconhecido do servidor")
                            } catch (e: Exception) {
                                errorStr.ifEmpty { "Erro do servidor" }
                            }
                            Toast.makeText(context, "Erro ao guardar: $userFriendlyMessage", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Sem ligação à internet ou erro no servidor: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isSaving = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(28.dp),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Salvar")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isIncome) "Guardar Receita" else "Salvar Despesa",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun formatToApiDate(input: String): String {
    return try {
        val parts = input.split("/")
        if (parts.size == 3) {
            val d = parts[0].padStart(2, '0')
            val m = parts[1].padStart(2, '0')
            val y = parts[2]
            "$y-$m-$d"
        } else {
            // Verificar se já está no formato correto yyyy-MM-dd
            if (input.contains("-") && input.split("-").size == 3) {
                input
            } else {
                "2026-05-23"
            }
        }
    } catch (e: Exception) {
        "2026-05-23"
    }
}
