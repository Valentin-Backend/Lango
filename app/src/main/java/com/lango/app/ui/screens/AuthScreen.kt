package com.lango.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lango.app.ui.theme.*
import com.lango.app.viewmodel.AuthState
import com.lango.app.viewmodel.AuthViewModel

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    var isLoginTab by remember { mutableStateOf(true) }
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }
    var regEmail by remember { mutableStateOf("") }
    var regLogin by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regConfirmVisible by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(AuthGradient)) {
        Box(
            modifier = Modifier.size(300.dp).offset(x = (-80).dp, y = (-80).dp)
                .background(androidx.compose.ui.graphics.Brush.radialGradient(
                    listOf(BrightPurple.copy(0.15f), Color.Transparent)), CircleShape)
        )
        Box(
            modifier = Modifier.size(250.dp).align(Alignment.BottomEnd).offset(x = 60.dp, y = 60.dp)
                .background(androidx.compose.ui.graphics.Brush.radialGradient(
                    listOf(MediumPurple.copy(0.12f), Color.Transparent)), CircleShape)
        )

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .systemBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Box(
                modifier = Modifier.size(72.dp).background(PrimaryGradient, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(Modifier.height(18.dp))
            Text("LANGO", fontSize = 30.sp, fontWeight = FontWeight.Bold,
                color = Color.White, letterSpacing = 4.sp)
            Text("Карточки на основе ИИ", fontSize = 13.sp, color = Color.White.copy(0.45f))

            Spacer(Modifier.height(36.dp))

            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(0.07f)).padding(4.dp)
            ) {
                Row {
                    TabChip("Вход", isLoginTab) { isLoginTab = true; viewModel.clearError() }
                    TabChip("Регистрация", !isLoginTab) { isLoginTab = false; viewModel.clearError() }
                }
            }

            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = isLoginTab,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                label = "auth-form"
            ) { login ->
                if (login) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AuthTextField(loginEmail, { loginEmail = it.trim(); viewModel.clearError() }, "Email",
                            Icons.Filled.Email, keyboardType = KeyboardType.Email, imeAction = ImeAction.Next,
                            onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        AuthTextField(loginPassword, { loginPassword = it; viewModel.clearError() }, "Пароль",
                            Icons.Filled.Lock, isPassword = true, passwordVisible = loginPasswordVisible,
                            onPasswordToggle = { loginPasswordVisible = !loginPasswordVisible },
                            imeAction = ImeAction.Done,
                            onDone = { focusManager.clearFocus(); viewModel.login(loginEmail, loginPassword) })
                        ErrorBanner(state)
                        Button(
                            onClick = { focusManager.clearFocus(); viewModel.login(loginEmail.trim(), loginPassword) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrightPurple),
                            enabled = state !is AuthState.Loading
                        ) {
                            if (state is AuthState.Loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text("Войти", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AuthTextField(regEmail, { regEmail = it.trim(); viewModel.clearError() }, "Email",
                            Icons.Filled.Email, keyboardType = KeyboardType.Email, imeAction = ImeAction.Next,
                            onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        AuthTextField(regLogin, {
                            regLogin = it.filter { c -> c.isLetterOrDigit() || c == '_' }.take(20)
                            viewModel.clearError()
                        }, "Логин", Icons.Filled.AlternateEmail, imeAction = ImeAction.Next,
                            onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        AuthTextField(regPassword, { regPassword = it.take(50); viewModel.clearError() },
                            "Пароль (мин. 6 символов)", Icons.Filled.Lock,
                            isPassword = true, passwordVisible = regPasswordVisible,
                            onPasswordToggle = { regPasswordVisible = !regPasswordVisible },
                            imeAction = ImeAction.Next, onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        AuthTextField(regConfirmPassword, { regConfirmPassword = it.take(50); viewModel.clearError() },
                            "Подтвердите пароль", Icons.Filled.LockOpen,
                            isPassword = true, passwordVisible = regConfirmVisible,
                            onPasswordToggle = { regConfirmVisible = !regConfirmVisible },
                            imeAction = ImeAction.Done,
                            onDone = { focusManager.clearFocus(); viewModel.register(regEmail, regPassword, regLogin) })
                        ErrorBanner(state)
                        val canRegister = regEmail.isNotBlank() && regLogin.isNotBlank() &&
                                regPassword.length >= 6 && regPassword == regConfirmPassword
                        Button(
                            onClick = { focusManager.clearFocus(); viewModel.register(regEmail.trim(), regPassword, regLogin.trim()) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrightPurple),
                            enabled = canRegister && state !is AuthState.Loading
                        ) {
                            if (state is AuthState.Loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text("Создать аккаунт", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(0.12f))
                Text("  или  ", color = Color.White.copy(0.3f), fontSize = 12.sp)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(0.12f))
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = { viewModel.continueAsGuest() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.18f))
            ) {
                Icon(Icons.Filled.PersonOutline, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Продолжить как гость", fontSize = 14.sp, color = Color.White.copy(0.65f))
            }
            Spacer(Modifier.height(16.dp))
            Text("Прогресс пользователя сохраняется только на устройстве",
                fontSize = 11.sp, color = Color.White.copy(0.3f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RowScope.TabChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
        .background(if (selected) BrightPurple else Color.Transparent),
        contentAlignment = Alignment.Center) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(42.dp)) {
            Text(text, fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.White else Color.White.copy(0.45f))
        }
    }
}

@Composable
private fun AuthTextField(
    value: String, onValueChange: (String) -> Unit, label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false, passwordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onNext: (() -> Unit)? = null, onDone: (() -> Unit)? = null,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(0.45f), fontSize = 12.sp) },
        leadingIcon = { Icon(icon, null, tint = BrightPurple.copy(0.8f)) },
        trailingIcon = if (isPassword && onPasswordToggle != null) {
            { IconButton(onClick = onPasswordToggle) {
                Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    null, tint = Color.White.copy(0.35f)) } }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onNext?.invoke() }, onDone = { onDone?.invoke() }),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            focusedBorderColor = BrightPurple, unfocusedBorderColor = Color.White.copy(0.18f),
            cursorColor = BrightPurple, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(14.dp),
        supportingText = if (supportingText != null) {{
            Text(supportingText, color = Color.White.copy(0.3f), fontSize = 10.sp)
        }} else null
    )
}

@Composable
private fun ErrorBanner(state: AuthState) {
    AnimatedVisibility(visible = state is AuthState.Error) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(ColorError.copy(0.12f)).padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.ErrorOutline, null, tint = ColorError, modifier = Modifier.size(16.dp))
                Text((state as? AuthState.Error)?.message ?: "", color = ColorError, fontSize = 13.sp)
            }
        }
    }
}
