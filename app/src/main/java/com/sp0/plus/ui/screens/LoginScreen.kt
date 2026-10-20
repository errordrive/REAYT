package com.sp0.plus.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sp0.plus.R
import com.sp0.plus.data.SettingsManager

enum class AuthStage {
    WELCOME,
    LOGIN,
    SIGNUP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var currentStage by remember { mutableStateOf(AuthStage.WELCOME) }

    // Form States
    var emailOrUsername by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Session Cookie Dialog
    var showSpDcDialog by remember { mutableStateOf(false) }
    var spDcInput by remember { mutableStateOf("") }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = Color(0xFF121212)
    ) {
        AnimatedContent(
            targetState = currentStage,
            transitionSpec = {
                if (targetState != AuthStage.WELCOME) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "AuthStageAnimation"
        ) { stage ->
            when (stage) {
                AuthStage.WELCOME -> {
                    WelcomeView(
                        onSignUpClick = {
                            errorMessage = null
                            currentStage = AuthStage.SIGNUP
                        },
                        onLoginClick = {
                            errorMessage = null
                            currentStage = AuthStage.LOGIN
                        },
                        onTelegramClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/sp0plus"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Opening Telegram community...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSessionCookieClick = {
                            showSpDcDialog = true
                        },
                        onGuestClick = {
                            SettingsManager.login(
                                username = "Guest Listener",
                                email = "guest@sp0plus.app",
                                tier = "Guest Mode (Offline Lossless)"
                            )
                            onLoginSuccess()
                        }
                    )
                }

                AuthStage.LOGIN -> {
                    LoginFormView(
                        emailOrUsername = emailOrUsername,
                        onEmailOrUsernameChange = {
                            emailOrUsername = it
                            errorMessage = null
                        },
                        password = password,
                        onPasswordChange = {
                            password = it
                            errorMessage = null
                        },
                        isPasswordVisible = isPasswordVisible,
                        onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                        rememberMe = rememberMe,
                        onRememberMeChange = { rememberMe = it },
                        errorMessage = errorMessage,
                        isLoading = isLoading,
                        onBack = { currentStage = AuthStage.WELCOME },
                        onLoginSubmit = {
                            focusManager.clearFocus()
                            if (emailOrUsername.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter both username and password."
                                return@LoginFormView
                            }
                            if (password.length < 4) {
                                errorMessage = "Password must be at least 4 characters."
                                return@LoginFormView
                            }

                            isLoading = true
                            val username = if (emailOrUsername.contains("@")) emailOrUsername.substringBefore("@") else emailOrUsername
                            val email = if (emailOrUsername.contains("@")) emailOrUsername else "$emailOrUsername@sp0plus.app"

                            SettingsManager.login(
                                username = username.replaceFirstChar { it.uppercase() },
                                email = email,
                                tier = "Sp0+ Lossless VIP (Permanent)"
                            )
                            isLoading = false
                            onLoginSuccess()
                        },
                        onOpenSpDc = { showSpDcDialog = true },
                        onForgotPassword = {
                            errorMessage = "Password reset instructions sent if account exists."
                        }
                    )
                }

                AuthStage.SIGNUP -> {
                    SignUpFormView(
                        displayName = displayName,
                        onDisplayNameChange = {
                            displayName = it
                            errorMessage = null
                        },
                        email = emailOrUsername,
                        onEmailChange = {
                            emailOrUsername = it
                            errorMessage = null
                        },
                        password = password,
                        onPasswordChange = {
                            password = it
                            errorMessage = null
                        },
                        isPasswordVisible = isPasswordVisible,
                        onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                        errorMessage = errorMessage,
                        isLoading = isLoading,
                        onBack = { currentStage = AuthStage.WELCOME },
                        onSignUpSubmit = {
                            focusManager.clearFocus()
                            if (emailOrUsername.isBlank() || password.isBlank()) {
                                errorMessage = "Please fill in all fields."
                                return@SignUpFormView
                            }
                            if (password.length < 4) {
                                errorMessage = "Password must be at least 4 characters."
                                return@SignUpFormView
                            }

                            isLoading = true
                            val user = if (displayName.isNotBlank()) displayName else emailOrUsername.substringBefore("@").replaceFirstChar { it.uppercase() }
                            val email = if (emailOrUsername.contains("@")) emailOrUsername else "$emailOrUsername@sp0plus.app"

                            SettingsManager.login(
                                username = user,
                                email = email,
                                tier = "Sp0+ Lossless VIP (Permanent)"
                            )
                            isLoading = false
                            onLoginSuccess()
                        }
                    )
                }
            }
        }

        // sp_dc Cookie Modal Dialog
        if (showSpDcDialog) {
            AlertDialog(
                onDismissRequest = { showSpDcDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = Color(0xFF1DB954))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Connect via sp_dc", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Paste your Spotify session cookie (sp_dc) to link your active Spotify session with Sp0+ Lossless Master VIP playback.",
                            color = Color(0xFFB3B3B3),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        OutlinedTextField(
                            value = spDcInput,
                            onValueChange = { spDcInput = it },
                            placeholder = { Text("Paste sp_dc cookie value...", color = Color(0xFF666666)) },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF1DB954),
                                unfocusedBorderColor = Color(0xFF444444)
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (spDcInput.isNotBlank()) {
                                SettingsManager.loginWithSpDc(spDcInput)
                                showSpDcDialog = false
                                Toast.makeText(context, "Lossless VIP Session Activated", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, "Please enter a valid cookie value", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                    ) {
                        Text("Connect", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSpDcDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF242424)
            )
        }
    }
}

@Composable
private fun WelcomeView(
    onSignUpClick: () -> Unit,
    onLoginClick: () -> Unit,
    onTelegramClick: () -> Unit,
    onSessionCookieClick: () -> Unit,
    onGuestClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Spotify Logo & Branding Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF1DB954),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.logo),
                        contentDescription = "Sp0+ Logo",
                        tint = Color.Black,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Millions of songs.",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 38.sp
            )
            Text(
                text = "Free on Sp0+.",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 38.sp
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Action Buttons Column
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Sign up free (Spotify Green Primary Pill)
            Button(
                onClick = onSignUpClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Sign up free",
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 2. Continue with Telegram (Bordered Pill)
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF1A1A1A),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(26.dp))
                    .clickable { onTelegramClick() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_telegram),
                        contentDescription = "Telegram",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Continue with Telegram",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 3. Continue with Session Cookie (sp_dc)
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF1A1A1A),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(26.dp))
                    .clickable { onSessionCookieClick() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Session",
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Log in with session (sp_dc)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 4. Continue as Guest (Bordered Pill)
            OutlinedButton(
                onClick = onGuestClick,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF555555)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Continue as Guest",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Log In (Text link at bottom)
            Text(
                text = "Log in",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onLoginClick() }
                    .padding(vertical = 12.dp, horizontal = 24.dp)
            )
        }
    }
}

@Composable
private fun LoginFormView(
    emailOrUsername: String,
    onEmailOrUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    rememberMe: Boolean,
    onRememberMeChange: (Boolean) -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onLoginSubmit: () -> Unit,
    onOpenSpDc: () -> Unit,
    onForgotPassword: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier.offset(x = (-8).dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Log in to Sp0+",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Username / Email input
        Text(
            text = "Email or username",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = emailOrUsername,
            onValueChange = onEmailOrUsernameChange,
            placeholder = { Text("Email or username", color = Color(0xFF666666), fontSize = 14.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF242424),
                unfocusedContainerColor = Color(0xFF242424),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF1DB954),
                unfocusedBorderColor = Color.Transparent
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Password input
        Text(
            text = "Password",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = { Text("Password", color = Color(0xFF666666), fontSize = 14.sp) },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color(0xFF888888)
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF242424),
                unfocusedContainerColor = Color(0xFF242424),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF1DB954),
                unfocusedBorderColor = Color.Transparent
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Remember Me Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Remember me",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Switch(
                checked = rememberMe,
                onCheckedChange = onRememberMeChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF1DB954),
                    uncheckedThumbColor = Color(0xFF888888),
                    uncheckedTrackColor = Color(0xFF333333)
                )
            )
        }

        // Error message banner
        errorMessage?.let { err ->
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (err.startsWith("Password reset")) Color(0xFF1E3A2F) else Color(0xFF3A1E1E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = err,
                    color = if (err.startsWith("Password reset")) Color(0xFF1DB954) else Color(0xFFFF5252),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Log In Button
        Button(
            onClick = onLoginSubmit,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    text = "Log In",
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Alternate Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Log in without password",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onForgotPassword() }
            )

            Text(
                text = "Use session cookie (sp_dc)",
                color = Color(0xFF1DB954),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onOpenSpDc() }
            )
        }
    }
}

@Composable
private fun SignUpFormView(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSignUpSubmit: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.offset(x = (-8).dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Create account",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Sign up for free lossless streaming on Sp0+",
            color = Color(0xFF888888),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Display Name
        Text(text = "What's your name?", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            placeholder = { Text("Display Name", color = Color(0xFF666666), fontSize = 14.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF242424),
                unfocusedContainerColor = Color(0xFF242424),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF1DB954),
                unfocusedBorderColor = Color.Transparent
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Email
        Text(text = "What's your email?", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = { Text("Email address", color = Color(0xFF666666), fontSize = 14.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF242424),
                unfocusedContainerColor = Color(0xFF242424),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF1DB954),
                unfocusedBorderColor = Color.Transparent
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Password
        Text(text = "Create a password", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = { Text("At least 4 characters", color = Color(0xFF666666), fontSize = 14.sp) },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color(0xFF888888)
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF242424),
                unfocusedContainerColor = Color(0xFF242424),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF1DB954),
                unfocusedBorderColor = Color.Transparent
            ),
            shape = RoundedCornerShape(8.dp)
        )

        errorMessage?.let { err ->
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF3A1E1E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = err,
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Create Account Button
        Button(
            onClick = onSignUpSubmit,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    text = "Create Account",
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
