package com.example.tripshot

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripshot.ui.theme.TripShotBgColor
import com.example.tripshot.ui.theme.TripShotHint
import com.example.tripshot.ui.theme.TripShotOnPrimary
import com.example.tripshot.ui.theme.TripShotOnSurface
import com.example.tripshot.ui.theme.TripShotPrimary
import com.example.tripshot.ui.theme.TripShotSurfaceColor
import com.example.tripshot.ui.theme.TripShotTabBgColor
import com.example.tripshot.ui.theme.TripShotTextPrimary
import com.example.tripshot.ui.theme.TripShotTextSecondary
import com.example.tripshot.ui.theme.TripShotTheme
import com.example.tripshot.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

enum class AuthScreen { LOGIN, SIGNUP }

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auto-login check: if user is already signed in, skip login screen
        if (FirebaseAuth.getInstance().currentUser != null) {
            navigateToMainActivity()
            return
        }

        enableEdgeToEdge()
        setContent {
            TripShotTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AuthRoot(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onAuthSuccess = { navigateToMainActivity() }
                    )
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

@Composable
fun AuthRoot(modifier: Modifier = Modifier, onAuthSuccess: () -> Unit = {}) {
    var currentScreen by rememberSaveable { mutableStateOf(AuthScreen.LOGIN) }

    Column(
        modifier = modifier
            .background(TripShotBgColor)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        // App title
        Text(
            text = stringResource(R.string.app_title),
            color = TripShotTextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Animated tab bar
        AnimatedTabBar(
            currentScreen = currentScreen,
            onTabSelected = { currentScreen = it }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Animated screen content
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                val toRight = targetState == AuthScreen.SIGNUP
                slideInHorizontally(
                    initialOffsetX = { if (toRight) it else -it },
                    animationSpec = tween(400)
                ) togetherWith slideOutHorizontally(
                    targetOffsetX = { if (toRight) -it else it },
                    animationSpec = tween(400)
                )
            },
            label = "auth_screen_transition"
        ) { screen ->
            when (screen) {
                AuthScreen.LOGIN -> LoginContent(
                    onGoToSignup = { currentScreen = AuthScreen.SIGNUP },
                    onAuthSuccess = onAuthSuccess
                )

                AuthScreen.SIGNUP -> SignupContent(
                    onGoToLogin = { currentScreen = AuthScreen.LOGIN },
                    onAuthSuccess = onAuthSuccess
                )
            }
        }
    }
}

@Composable
fun AnimatedTabBar(
    currentScreen: AuthScreen,
    onTabSelected: (AuthScreen) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(100.dp))
            .background(TripShotTabBgColor)
            .padding(6.dp)
    ) {
        val tabWidth: Dp = (this.maxWidth - 12.dp) / 2  // <-- use this.maxWidth

        val indicatorOffset by animateDpAsState(
            targetValue = if (currentScreen == AuthScreen.LOGIN) 0.dp else tabWidth,
            animationSpec = tween(durationMillis = 300),
            label = "tab_indicator"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .height(40.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(TripShotPrimary)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf(AuthScreen.LOGIN to stringResource(R.string.tab_login), AuthScreen.SIGNUP to stringResource(R.string.tab_signup))
                .forEach { (screen, label) ->
                    val isSelected = currentScreen == screen
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(tabWidth)
                            .height(40.dp)
                    ) {
                        TextButton(
                            onClick = { onTabSelected(screen) },
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (isSelected) {
                                    TripShotOnPrimary
                                } else {
                                    TripShotTextSecondary
                                }
                            )
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
        }
    }
}

@Composable
fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = TripShotSurfaceColor,
    unfocusedContainerColor = TripShotSurfaceColor,
    disabledContainerColor = TripShotSurfaceColor,
    errorContainerColor = TripShotSurfaceColor,
    focusedBorderColor = TripShotPrimary,
    unfocusedBorderColor = Color.Transparent,
    errorBorderColor = Color.Transparent,
    cursorColor = TripShotOnSurface,
    focusedTextColor = TripShotTextPrimary,
    unfocusedTextColor = TripShotTextPrimary,
    focusedPlaceholderColor = TripShotHint,
    unfocusedPlaceholderColor = TripShotHint,
    focusedLabelColor = TripShotTextSecondary,
    unfocusedLabelColor = TripShotHint,
    errorLeadingIconColor = MaterialTheme.colorScheme.error,
    errorTrailingIconColor = TripShotHint,
    errorLabelColor = MaterialTheme.colorScheme.error,
    errorSupportingTextColor = MaterialTheme.colorScheme.error
)

@Composable
fun LoginContent(onGoToSignup: () -> Unit, onAuthSuccess: () -> Unit = {}) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    val emailError = showErrors && email.isBlank()
    val passwordError = showErrors && password.isBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.welcome_back),
            color = TripShotTextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.welcome_subtitle),
            color = TripShotTextSecondary,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.label_email),
            color = if (emailError) MaterialTheme.colorScheme.error else TripShotTextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(100.dp), clip = false),
            placeholder = { Text(stringResource(R.string.placeholder_email), color = TripShotHint) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = stringResource(R.string.content_desc_email),
                    tint = if (emailError) MaterialTheme.colorScheme.error else TripShotHint
                )
            },
            shape = RoundedCornerShape(100.dp),
            colors = authFieldColors(),
            enabled = !isLoading,
            isError = emailError,
            supportingText = if (emailError) {
                {
                    Text(
                        text = stringResource(R.string.error_email_required),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else null
        )

        Spacer(modifier = Modifier.height(if (emailError) 8.dp else 20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.label_password),
                color = if (passwordError) MaterialTheme.colorScheme.error else TripShotTextSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = {}) {
                Text(stringResource(R.string.forgot_password), color = TripShotPrimary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(100.dp), clip = false),
            placeholder = { Text(stringResource(R.string.placeholder_password), color = TripShotHint) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.content_desc_password),
                    tint = if (passwordError) MaterialTheme.colorScheme.error else TripShotHint
                )
            },
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password),
                        color = TripShotHint
                    )
                }
            },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            shape = RoundedCornerShape(100.dp),
            colors = authFieldColors(),
            enabled = !isLoading,
            isError = passwordError,
            supportingText = if (passwordError) {
                {
                    Text(
                        text = stringResource(R.string.error_password_required),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else null
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                showErrors = true
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isLoading = true
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            onAuthSuccess()
                        } else {
                            Toast.makeText(
                                context,
                                "Login failed: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(100.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TripShotPrimary,
                contentColor = TripShotOnPrimary
            ),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = TripShotOnPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(R.string.login_button), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.no_account_text), color = TripShotTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(
                onClick = onGoToSignup,
                contentPadding = PaddingValues(0.dp),
                enabled = !isLoading
            ) {
                Text(
                    text = stringResource(R.string.signup_link),
                    color = TripShotPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SignupContent(onGoToLogin: () -> Unit, onAuthSuccess: () -> Unit = {}) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    val nameError = showErrors && name.isBlank()
    val emailError = showErrors && email.isBlank()
    val passwordError = showErrors && password.isBlank()
    val confirmError = showErrors && (confirmPassword.isBlank() || confirmPassword != password)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.create_account_title),
            color = TripShotTextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.signup_subtitle),
            color = TripShotTextSecondary,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.label_full_name),
            color = if (nameError) MaterialTheme.colorScheme.error else TripShotTextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(100.dp), clip = false),
            placeholder = { Text(stringResource(R.string.placeholder_name), color = TripShotHint) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(R.string.content_desc_name),
                    tint = if (nameError) MaterialTheme.colorScheme.error else TripShotHint
                )
            },
            shape = RoundedCornerShape(100.dp),
            colors = authFieldColors(),
            enabled = !isLoading,
            isError = nameError,
            supportingText = if (nameError) {
                {
                    Text(
                        text = stringResource(R.string.error_name_required),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else null
        )

        Spacer(modifier = Modifier.height(if (nameError) 8.dp else 20.dp))

        Text(
            text = stringResource(R.string.label_email),
            color = if (emailError) MaterialTheme.colorScheme.error else TripShotTextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(100.dp), clip = false),
            placeholder = { Text(stringResource(R.string.placeholder_email), color = TripShotHint) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = stringResource(R.string.content_desc_email),
                    tint = if (emailError) MaterialTheme.colorScheme.error else TripShotHint
                )
            },
            shape = RoundedCornerShape(100.dp),
            colors = authFieldColors(),
            enabled = !isLoading,
            isError = emailError,
            supportingText = if (emailError) {
                {
                    Text(
                        text = stringResource(R.string.error_email_required),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else null
        )

        Spacer(modifier = Modifier.height(if (emailError) 8.dp else 20.dp))

        Text(
            text = stringResource(R.string.label_password),
            color = if (passwordError) MaterialTheme.colorScheme.error else TripShotTextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(100.dp), clip = false),
            placeholder = { Text(stringResource(R.string.placeholder_password), color = TripShotHint) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.content_desc_password),
                    tint = if (passwordError) MaterialTheme.colorScheme.error else TripShotHint
                )
            },
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password),
                        color = TripShotHint
                    )
                }
            },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            shape = RoundedCornerShape(100.dp),
            colors = authFieldColors(),
            enabled = !isLoading,
            isError = passwordError,
            supportingText = if (passwordError) {
                {
                    Text(
                        text = stringResource(R.string.error_password_required),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else null
        )

        Spacer(modifier = Modifier.height(if (passwordError) 8.dp else 20.dp))

        Text(
            text = stringResource(R.string.label_confirm_password),
            color = if (confirmError) MaterialTheme.colorScheme.error else TripShotTextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(100.dp), clip = false),
            placeholder = { Text(stringResource(R.string.placeholder_password), color = TripShotHint) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.content_desc_confirm_password),
                    tint = if (confirmError) MaterialTheme.colorScheme.error else TripShotHint
                )
            },
            trailingIcon = {
                TextButton(onClick = { confirmVisible = !confirmVisible }) {
                    Text(
                        text = if (confirmVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password),
                        color = TripShotHint
                    )
                }
            },
            visualTransformation = if (confirmVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            shape = RoundedCornerShape(100.dp),
            colors = authFieldColors(),
            enabled = !isLoading,
            isError = confirmError,
            supportingText = if (confirmError) {
                {
                    Text(
                        text = if (confirmPassword.isBlank()) stringResource(R.string.error_password_required) else stringResource(R.string.error_passwords_dont_match),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else null
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                showErrors = true
                if (email.isBlank() || password.isBlank() || name.isBlank() || confirmPassword.isBlank()) {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (password != confirmPassword) {
                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isLoading = true
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null) {
                                val profileUpdates = userProfileChangeRequest {
                                    displayName = name
                                }
                                user.updateProfile(profileUpdates)
                                    .addOnCompleteListener { _ ->

                                        val db = FirebaseFirestore.getInstance()
                                        val userObj = User(
                                            uid = user.uid,
                                            name = name,
                                            email = email
                                        )

                                        db.collection("users").document(user.uid)
                                            .set(userObj)
                                            .addOnCompleteListener { _ ->
                                                isLoading = false
                                                onAuthSuccess()
                                            }
                                            .addOnFailureListener { e ->
                                                isLoading = false
                                                Toast.makeText(context, "Firestore error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                // Still proceed to main activity as auth was successful
                                                onAuthSuccess()
                                            }
                                    }
                            }
                            else {
                                isLoading = false
                                onAuthSuccess()
                            }
                        } else {
                            isLoading = false
                            Toast.makeText(
                                context,
                                "Signup failed: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(100.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TripShotPrimary,
                contentColor = TripShotOnPrimary
            ),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = TripShotOnPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(R.string.signup_button), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.have_account_text), color = TripShotTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(
                onClick = onGoToLogin,
                contentPadding = PaddingValues(0.dp),
                enabled = !isLoading
            ) {
                Text(
                    text = stringResource(R.string.login_link),
                    color = TripShotPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun AuthRootPreview() {
    TripShotTheme {
        AuthRoot()
    }
}