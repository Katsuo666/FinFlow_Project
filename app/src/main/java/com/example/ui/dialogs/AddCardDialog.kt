package com.example.ui.dialogs

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.RetrofitClient
import com.example.model.Card
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AddCardDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onCardAdded: () -> Unit,
    context: Context,
    coroutineScope: CoroutineScope
) {
    if (!showDialog) return

    var cardName by remember { mutableStateOf("") }
    var cardholderName by remember { mutableStateOf("") }
    var digits by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("CREDIT") }
    var limitAmtStr by remember { mutableStateOf("") }
    var currentBillStr by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("VISA") }
    var isSavingCard by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vincular Novo Cartão", color = Color.White, fontWeight = FontWeight.Bold) },
        containerColor = Color(0xFF1E1E1E),
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
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
                                onDismiss()
                                onCardAdded()
                            } else {
                                Toast.makeText(context, "Erro do servidor.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Erro de ligação. Cartão simulado offline.", Toast.LENGTH_SHORT).show()
                            onDismiss()
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
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}
