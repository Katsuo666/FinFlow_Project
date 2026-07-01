package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.RetrofitClient
import com.example.model.Goal
import com.example.utils.CurrencyFormatter
import kotlinx.coroutines.launch
import java.util.Locale

// ---------------- ECRÃ DO SEPARADOR METAS ----------------
@Composable
fun MetasScreen(goals: List<Goal>, onGoalUpdated: () -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currencyFormat = remember { CurrencyFormatter.getCurrencyFormat() }

    var goalToEdit by remember { mutableStateOf<Goal?>(null) }
    var newCurrentAmount by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }
    
    var goalToDelete by remember { mutableStateOf<Goal?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    var selectedFilter by remember { mutableStateOf("Tudo") }

    val activeCount = goals.count { it.currentAmount < it.targetAmount }
    val completedCount = goals.count { it.currentAmount >= it.targetAmount }

    val filteredGoals = remember(goals, selectedFilter) {
        when (selectedFilter) {
            "Ativas" -> goals.filter { it.currentAmount < it.targetAmount }
            "Concluídas" -> goals.filter { it.currentAmount >= it.targetAmount }
            else -> goals
        }
    }

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

        // Removido o Cartão do Total Guardado conforme solicitado

        // Caixas de estatísticas
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedFilter = if (selectedFilter == "Ativas") "Tudo" else "Ativas" },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedFilter == "Ativas") Color(0xFF2C2C2C) else Color(0xFF1E1E1E)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (selectedFilter == "Ativas") BorderStroke(1.dp, Color(0xFF00E676)) else null
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Flag, contentDescription = "Ativas", tint = Color(0xFF00E676))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(activeCount.toString(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Metas Ativas", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedFilter = if (selectedFilter == "Concluídas") "Tudo" else "Concluídas" },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedFilter == "Concluídas") Color(0xFF2C2C2C) else Color(0xFF1E1E1E)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (selectedFilter == "Concluídas") BorderStroke(1.dp, Color(0xFF45FEC9)) else null
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Concluídas", tint = Color(0xFF45FEC9))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(completedCount.toString(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Concluída", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }

        // Detalhes das metas
        if (filteredGoals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedFilter == "Tudo") "Nenhuma meta cadastrada" else "Nenhuma meta nesta categoria",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            items(filteredGoals) { goal ->
                val isCompleted = goal.currentAmount >= goal.targetAmount
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            goalToEdit = goal
                            newCurrentAmount = goal.currentAmount.toString()
                        },
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
                                        .background(if (isCompleted) Color(0xFF003918) else Color(0xFF2C2C2C), shape = RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Star, 
                                        contentDescription = "Goal", 
                                        tint = if (isCompleted) Color(0xFF00E676) else Color(0xFF00E676)
                                    )
                                }
                                Column {
                                    Text(goal.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF003918), shape = RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Progresso", color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        if (!goal.deadline.isNullOrEmpty()) {
                                            Text(goal.deadline, color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                            
                            // Botão de eliminar (visível se concluída ou opcional para todas)
                            if (isCompleted) {
                                IconButton(onClick = { goalToDelete = goal }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFFB4AB))
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
                                color = if (isCompleted) Color(0xFF00E676) else Color(0xFF00E676),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = if (isCompleted) Color(0xFF00E676) else Color(0xFF00E676),
                            trackColor = Color(0xFF2C2C2C),
                        )
                    }
                }
            }
        }
    }

    // Diálogo para editar progresso da meta
    if (goalToEdit != null) {
        AlertDialog(
            onDismissRequest = { if (!isUpdating) goalToEdit = null },
            title = { Text("Atualizar Progresso", color = Color.White) },
            text = {
                Column {
                    Text("Meta: ${goalToEdit?.name}", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newCurrentAmount,
                        onValueChange = { newCurrentAmount = it },
                        label = { Text("Valor Guardado") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = newCurrentAmount.toDoubleOrNull() ?: 0.0
                        if (goalToEdit != null) {
                            if (amt > goalToEdit!!.targetAmount) {
                                Toast.makeText(context, "O valor não pode ultrapassar a meta (${currencyFormat.format(goalToEdit!!.targetAmount)})", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isUpdating = true
                            coroutineScope.launch {
                                try {
                                    val api = RetrofitClient.getApiService(context)
                                    val updatedGoal = goalToEdit!!.copy(currentAmount = amt)
                                    val response = api.updateGoal(updatedGoal.id!!, updatedGoal)
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "Meta atualizada!", Toast.LENGTH_SHORT).show()
                                        onGoalUpdated()
                                        goalToEdit = null
                                    } else {
                                        Toast.makeText(context, "Erro ao atualizar meta", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro de rede", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isUpdating = false
                                }
                            }
                        }
                    },
                    enabled = !isUpdating
                ) {
                    if (isUpdating) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Salvar")
                }
            },
            dismissButton = {
                Row {
                    // Botão de eliminar dentro do edit (opcional)
                    TextButton(onClick = { 
                        goalToDelete = goalToEdit
                        goalToEdit = null
                    }) {
                        Text("Eliminar", color = Color(0xFFFFB4AB))
                    }
                    TextButton(onClick = { goalToEdit = null }, enabled = !isUpdating) {
                        Text("Cancelar", color = Color.LightGray)
                    }
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    // Diálogo de confirmação de eliminação
    if (goalToDelete != null) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) goalToDelete = null },
            title = { Text("Eliminar Meta", color = Color.White) },
            text = { Text("Tem a certeza que deseja eliminar a meta '${goalToDelete?.name}'?", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        if (goalToDelete != null) {
                            isDeleting = true
                            coroutineScope.launch {
                                try {
                                    val api = RetrofitClient.getApiService(context)
                                    val response = api.deleteGoal(goalToDelete!!.id!!)
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "Meta eliminada!", Toast.LENGTH_SHORT).show()
                                        onGoalUpdated()
                                        goalToDelete = null
                                    } else {
                                        Toast.makeText(context, "Erro ao eliminar", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro de rede", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isDeleting = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }, enabled = !isDeleting) {
                    Text("Cancelar", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }
}
