package com.example.ui.dialogs

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.RetrofitClient
import com.example.model.Card
import com.example.utils.CurrencyFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CardDetailsDialog(
    card: Card?,
    onDismiss: () -> Unit,
    onCardDeleted: () -> Unit,
    context: Context,
    coroutineScope: CoroutineScope,
    onRemoveCard: (Card) -> Unit
) {
    if (card == null) return

    var isDeletingCard by remember { mutableStateOf(false) }
    val currencyFormat = CurrencyFormatter.getCurrencyFormat()

    AlertDialog(
        onDismissRequest = onDismiss,
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
                                    onDismiss()
                                    onCardDeleted()
                                } else {
                                    Toast.makeText(context, "Erro ao desvincular.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                onRemoveCard(card)
                                onDismiss()
                                Toast.makeText(context, "Desvinculado localmente.", Toast.LENGTH_SHORT).show()
                            } finally {
                                isDeletingCard = false
                            }
                        }
                    } else {
                        onRemoveCard(card)
                        onDismiss()
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
            TextButton(onClick = onDismiss) {
                Text("Voltar", color = Color.Gray)
            }
        }
    )
}
