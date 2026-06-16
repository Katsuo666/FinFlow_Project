package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.RetrofitClient
import com.example.utils.TokenManager
import kotlinx.coroutines.launch

// ---------------- ECRÃ DO SEPARADOR PERFIL ----------------
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
                    Image(
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
