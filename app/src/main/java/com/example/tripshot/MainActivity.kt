package com.example.tripshot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripshot.ui.theme.TripShotTheme
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.Dp

// ─── Colours ──────────────────────────────────────────────────────────────────
private val BgColor      = Color(0xFF1A1C1A)
private val SurfaceColor = Color(0xFF333633)
private val TabBgColor   = Color(0xFF282B28)
private val Primary      = Color(0xFFFF7D00)
private val TextPrimary  = Color(0xFFE1E3DF)
private val TextSecondary = Color(0xFFC1C9C0)
private val Hint         = Color(0xFF8B938B)
private val DividerColor = Color(0xFF444844)

// ─── Screen enum ──────────────────────────────────────────────────────────────
enum class AuthScreen { LOGIN, SIGNUP }

// ─── Activity ─────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TripShotTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AuthRoot(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

// ─── Root that owns the current-screen state ──────────────────────────────────
@Composable
fun AuthRoot(modifier: Modifier = Modifier) {
    var currentScreen by rememberSaveable { mutableStateOf(AuthScreen.LOGIN) }

    Column(
        modifier = modifier
            .background(BgColor)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        // App title
        Text(
            text = "TripShot",
            color = TextPrimary,
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
                    onGoToSignup = { currentScreen = AuthScreen.SIGNUP }
                )
                AuthScreen.SIGNUP -> SignupContent(
                    onGoToLogin = { currentScreen = AuthScreen.LOGIN }
                )
            }
        }
    }
}

// ─── Animated pill tab bar ────────────────────────────────────────────────────
@Composable
fun AnimatedTabBar(
    currentScreen: AuthScreen,
    onTabSelected: (AuthScreen) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(100.dp))
            .background(TabBgColor)
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
                .background(Primary)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf(AuthScreen.LOGIN to "Login", AuthScreen.SIGNUP to "Sign up")
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
                                    Color.White
                                } else {
                                    TextSecondary
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

// ─── Shared text field colours helper ─────────────────────────────────────────
@Composable
fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = SurfaceColor,
    unfocusedContainerColor = SurfaceColor,
    disabledContainerColor = SurfaceColor,
    focusedBorderColor = Primary,
    unfocusedBorderColor = SurfaceColor,
    cursorColor = Color.White,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedPlaceholderColor = Hint,
    unfocusedPlaceholderColor = Hint,
    focusedLabelColor = TextSecondary,
    unfocusedLabelColor = Hint
)

// ─── Login screen ─────────────────────────────────────────────────────────────
@Composable
fun LoginContent(onGoToSignup: () -> Unit) {
    var email    by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Welcome Back",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your details to continue your journey.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "EMAIL ADDRESS",
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("alex@explorer.com", color = Hint) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email",
                    tint = Hint
                )
            },
            shape = RoundedCornerShape(100.dp),
            colors = authFieldColors()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PASSWORD",
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = {}) {
                Text("Forgot?", color = Primary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("••••••••", color = Hint) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Password",
                    tint = Hint
                )
            },
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) "Hide" else "Show",
                        color = Hint
                    )
                }
            },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            shape = RoundedCornerShape(100.dp),
            colors = authFieldColors()
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = { /* TODO: handle login */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(100.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = Color.White
            )
        ) {
            Text("Start Exploring", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(28.dp))

        OrDivider()

        Spacer(modifier = Modifier.height(20.dp))

        SocialButtons()

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Don't have an account? ", color = TextSecondary)
            TextButton(
                onClick = onGoToSignup,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(
                    text = "Join the community",
                    color = Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── Sign up screen ───────────────────────────────────────────────────────────
@Composable
fun SignupContent(onGoToLogin: () -> Unit) {
    var name            by rememberSaveable { mutableStateOf("") }
    var email           by rememberSaveable { mutableStateOf("") }
    var password        by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible  by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Create Account",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Join the community and start your journey.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "FULL NAME",
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Alex Explorer", color = Hint) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Name",
                    tint = Hint
                )
            },
            shape = RoundedCornerShape(100.dp),
            colors = authFieldColors()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "EMAIL ADDRESS",
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("alex@explorer.com", color = Hint) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email",
                    tint = Hint
                )
            },
            shape = RoundedCornerShape(100.dp),
            colors = authFieldColors()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "PASSWORD",
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("••••••••", color = Hint) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Password",
                    tint = Hint
                )
            },
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) "Hide" else "Show",
                        color = Hint
                    )
                }
            },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            shape = RoundedCornerShape(100.dp),
            colors = authFieldColors()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "CONFIRM PASSWORD",
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("••••••••", color = Hint) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Confirm Password",
                    tint = Hint
                )
            },
            trailingIcon = {
                TextButton(onClick = { confirmVisible = !confirmVisible }) {
                    Text(
                        text = if (confirmVisible) "Hide" else "Show",
                        color = Hint
                    )
                }
            },
            visualTransformation = if (confirmVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            shape = RoundedCornerShape(100.dp),
            colors = authFieldColors()
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = { /* TODO: handle signup */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(100.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = Color.White
            )
        ) {
            Text("Join the Community", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(28.dp))

        OrDivider()

        Spacer(modifier = Modifier.height(20.dp))

        SocialButtons()

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account? ", color = TextSecondary)
            TextButton(
                onClick = onGoToLogin,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(
                    text = "Log in",
                    color = Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── Shared composables ───────────────────────────────────────────────────────
@Composable
fun OrDivider() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Divider(modifier = Modifier.weight(1f), color = DividerColor)
        Text(
            text = " OR CONTINUE WITH ",
            color = Hint,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Divider(modifier = Modifier.weight(1f), color = DividerColor)
    }
}

@Composable
fun SocialButtons() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = { /* TODO: Google auth */ },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(100.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SurfaceColor,
                contentColor = TextPrimary
            )
        ) {
            Text("Google")
        }

        Button(
            onClick = { /* TODO: Apple auth */ },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(100.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SurfaceColor,
                contentColor = TextPrimary
            )
        ) {
            Text("Apple")
        }
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun AuthRootPreview() {
    TripShotTheme {
        AuthRoot()
    }
}