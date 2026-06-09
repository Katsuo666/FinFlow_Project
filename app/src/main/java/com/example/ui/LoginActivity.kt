package com.example.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import com.example.api.RetrofitClient
import com.example.model.LoginRequest
import com.example.model.RegisterRequest
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.TokenManager
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        tokenManager = TokenManager(this)

        // Se já estiver logado, ignorar a LoginActivity
        if (tokenManager.getToken() != null) {
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
                            .padding(innerPadding)
                    ) {
                        LoginScreenContent(
                            onAuthSuccess = { token, name, email ->
                                tokenManager.saveToken(token)
                                tokenManager.saveUser(name, email)
                                Toast.makeText(this@LoginActivity, "Bem-vindo!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                finish()
                            }
                        )
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
        // Rótulo limpo e de alto contraste acima/fora do campo de texto
        Text(
            text = labelText,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Contentor retangular branco sólido com texto escuro
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
                focusedBorderColor = Color(0xFF00FF66), // cor de foco verde néon
                unfocusedBorderColor = Color.Transparent, // contorno transparente quando sem foco
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

    var isLoginMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Alteração suave da cor de fundo baseada no modo do ecrã
    val backgroundColor by animateColorAsState(
        targetValue = if (isLoginMode) Color(0xFF121212) else Color(0xFF022B17),
        animationSpec = tween(durationMillis = 350),
        label = "BgColorAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Emblema/Ícone da Aplicação
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

            // Nome da Marca da Aplicação - buildAnnotatedString divide a cor exatamente como nas imagensagens
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

            // Título do Ecrã
            Text(
                text = if (isLoginMode) "Login" else "Criar Conta",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(28.dp))

            // --- Contentor de campos ---
            if (isLoginMode) {
                // Campos de Login
                FinFlowInputField(
                    value = email,
                    onValueChange = { email = it },
                    labelText = "Digite seu e-mail *",
                    placeholderText = "exemplo@email.com",
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
            } else {
                // Campos de Registo
                FinFlowInputField(
                    value = email,
                    onValueChange = { email = it },
                    labelText = "Seu Email *",
                    placeholderText = "exemplo@email.com",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(16.dp))

                FinFlowInputField(
                    value = name,
                    onValueChange = { name = it },
                    labelText = "Nome *"
                )

                Spacer(modifier = Modifier.height(16.dp))

                FinFlowInputField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    labelText = "Sobrenome *"
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

                Spacer(modifier = Modifier.height(16.dp))

                FinFlowInputField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    labelText = "Confirme sua senha *",
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Confirmar senha",
                                tint = Color.Gray
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botão de Ação
            // Botão de login -> fundo verde brilhante com texto preto/verde escuro profundo
            // Botão de registo -> fundo branco sólido com texto verde escuro
            val buttonColor = if (isLoginMode) Color(0xFF00FF66) else Color.White
            val buttonContentColor = if (isLoginMode) Color(0xFF121212) else Color(0xFF022B17)

            Button(
                onClick = {
                    if (isLoading) return@Button

                    // Validações básicas
                    if (email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(context, "Por favor preencha todos os campos", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!isLoginMode) {
                        if (name.isEmpty() || lastName.isEmpty()) {
                            Toast.makeText(context, "Por favor preencha o Nome e Sobrenome", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (password != confirmPassword) {
                            Toast.makeText(context, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                    }

                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val apiService = RetrofitClient.getApiService(context)
                            if (isLoginMode) {
                                val response = apiService.login(LoginRequest(email, password))
                                if (response.isSuccessful && response.body() != null) {
                                    val authBody = response.body()!!
                                    val token = authBody.token
                                    if (!token.isNullOrEmpty()) {
                                        onAuthSuccess(token, authBody.name, authBody.email)
                                    } else {
                                        Toast.makeText(context, "Erro: Sem token na resposta", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    val errorMsg = response.errorBody()?.string() ?: "Credenciais inválidas"
                                    Toast.makeText(context, "Erro no login: $errorMsg", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                val fullName = "$name $lastName"
                                val response = apiService.register(RegisterRequest(fullName, email, password))
                                if (response.isSuccessful && response.body() != null) {
                                    val authBody = response.body()!!
                                    val token = authBody.token
                                    if (!token.isNullOrEmpty()) {
                                        onAuthSuccess(token, authBody.name, authBody.email)
                                    } else {
                                        Toast.makeText(context, "Registo efetuado! Faça login.", Toast.LENGTH_SHORT).show()
                                        isLoginMode = true
                                    }
                                } else {
                                    val errorMsg = response.errorBody()?.string() ?: "Dados de registo inválidos"
                                    Toast.makeText(context, "Erro no registo: $errorMsg", Toast.LENGTH_LONG).show()
                                }
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
                    containerColor = buttonColor,
                    contentColor = buttonContentColor
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = buttonContentColor, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (isLoginMode) "Entrar" else "Registrar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Link do sub-botão: "Esqueci a Palavra-passe?" apenas visível no ecrã de Login
            if (isLoginMode) {
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
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Gatilho de mudança de modo na parte inferior
            Row(
                modifier = Modifier
                    .clickable { isLoginMode = !isLoginMode }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLoginMode) "Não tem conta? " else "Já tem conta? ",
                    color = if (isLoginMode) Color.Gray else Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Text(
                    text = if (isLoginMode) "Crie uma agora" else "Fazer Login",
                    color = if (isLoginMode) Color(0xFF00FF66) else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
