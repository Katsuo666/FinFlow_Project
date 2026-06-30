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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.api.RetrofitClient
import com.example.model.LoginRequest
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.TokenManager
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        tokenManager = TokenManager(this)

        // Limpar o cache do Retrofit para garantir que novos logins não usem dados antigos
        RetrofitClient.reset()

        // Se já estiver logado, ignorar a LoginActivity
        val isLoggingOut = intent.getBooleanExtra("LOGOUT", false)
        if (!isLoggingOut && tokenManager.getToken() != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            MyApplicationTheme {
                Scaffold { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        LoginScreenContent { token, name, email ->
                            tokenManager.saveToken(token)
                            
                            // Usar lifecycleScope para buscar o perfil completo ANTES de entrar
                            // Isto garante que a Bio e outros dados são carregados do servidor no login
                            lifecycleScope.launch {
                                try {
                                    val api = RetrofitClient.getApiService(this@LoginActivity)
                                    val profileRes = api.getUserProfile()
                                    if (profileRes.isSuccessful && profileRes.body() != null) {
                                        val p = profileRes.body()!!
                                        // Guardar todos os campos, incluindo a Bio vinda do servidor
                                        tokenManager.saveUser(p.name ?: name, p.email ?: email, p.bio)
                                        // Sincronizar também a imagem de perfil
                                        if (!p.avatarUrl.isNullOrEmpty()) {
                                            tokenManager.saveProfilePicture(p.avatarUrl)
                                        }
                                    } else {
                                        tokenManager.saveUser(name, email)
                                    }
                                } catch (_: Exception) {
                                    tokenManager.saveUser(name, email)
                                } finally {
                                    Toast.makeText(this@LoginActivity, "Bem-vindo!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                    finish()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinFlowInputField(
    value: String,
    onValueChange: (String) -> Unit,
    labelText: String,
    placeholderText: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = labelText,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            trailingIcon = trailingIcon,
            placeholder = if (placeholderText.isNotEmpty()) {
                { Text(placeholderText, color = Color.Gray, fontSize = 14.sp) }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFF00FF66),
                unfocusedBorderColor = Color.Transparent,
                cursorColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun LoginScreenContent(onAuthSuccess: (String, String?, String?) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(value = false) }
    var isLoading by remember { mutableStateOf(value = false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF00E676), Color(0xFF014D23))
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .border(1.5.dp, Color(0xFF00FF66), shape = RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$",
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = buildAnnotatedString {
                    append("Fin")
                    withStyle(style = SpanStyle(color = Color(0xFF00FF66))) {
                        append("Flow")
                    }
                },
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Login",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(28.dp))

            FinFlowInputField(
                value = email,
                onValueChange = { email = it },
                labelText = "Digite seu e-mail *",
                placeholderText = "seu@email.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            FinFlowInputField(
                value = password,
                onValueChange = { password = it },
                labelText = "Digite sua senha *",
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Mostrar senha",
                            tint = Color.Gray
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isLoading) return@Button

                    val trimmedEmail = email.trim()
                    val trimmedPassword = password.trim()

                    if (trimmedEmail.isEmpty() || trimmedPassword.isEmpty()) {
                        Toast.makeText(context, "Por favor preencha todos os campos", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val apiService = RetrofitClient.getApiService(context)
                            val response = apiService.login(LoginRequest(trimmedEmail, trimmedPassword))
                            if (response.isSuccessful && response.body() != null) {
                                val authBody = response.body()!!
                                val token = authBody.token?.replace("\"", "")?.trim()
                                if (!token.isNullOrEmpty()) {
                                    val displayName = authBody.name ?: "Utilizador"
                                    onAuthSuccess(token, displayName, authBody.email ?: trimmedEmail)
                                } else {
                                    Toast.makeText(context, "Erro: Sem token na resposta", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                val errorRaw = response.errorBody()?.string() ?: ""
                                val errorDisplay = if (errorRaw.contains("Utilizador não encontrado")) {
                                    "Utilizador não encontrado. Verifique o e-mail ou registe-se."
                                } else {
                                    errorRaw.ifEmpty { "Credenciais inválidas" }
                                }
                                Toast.makeText(context, "Erro no login: $errorDisplay", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Erro de rede: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00FF66),
                    contentColor = Color(0xFF121212)
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color(0xFF121212), modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Entrar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Esqueci o Password?",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .clickable {
                        Toast.makeText(context, "Mecanismo de recuperação em desenvolvimento.", Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .clickable {
                        val intent = Intent(context, RegisterActivity::class.java)
                        context.startActivity(intent)
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Não tem conta? ",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Text(
                    text = "Crie uma agora",
                    color = Color(0xFF00FF66),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
