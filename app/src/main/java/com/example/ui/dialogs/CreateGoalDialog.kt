package com.example.ui.dialogs

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.api.RetrofitClient
import com.example.model.Goal
import com.example.utils.CurrencyFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*

@Composable
fun CreateGoalDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onGoalCreated: () -> Unit,
    context: Context,
    coroutineScope: CoroutineScope
) {
    if (!showDialog) return

    var goalName by remember { mutableStateOf("") }
    var targetAmtStr by remember { mutableStateOf("") }
    var currentAmtStr by remember { mutableStateOf("") }
    var deadlineStr by remember { mutableStateOf("") }
    var isSavingGoal by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                                onDismiss()
                                onGoalCreated()
                            } else {
                                Toast.makeText(context, "Por favor tente novamente.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Sem ligação à internet. Tentando offline.", Toast.LENGTH_SHORT).show()
                            onDismiss()
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
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}
