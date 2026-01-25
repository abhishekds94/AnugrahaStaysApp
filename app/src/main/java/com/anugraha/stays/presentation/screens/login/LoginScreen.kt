package com.anugraha.stays.presentation.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anugraha.stays.presentation.components.AnugrahaPasswordTextField
import com.anugraha.stays.presentation.components.AnugrahaPrimaryButton
import com.anugraha.stays.presentation.components.AnugrahaTextField
import com.anugraha.stays.presentation.components.LoadingScreen
import com.anugraha.stays.presentation.theme.AnugrahaStaysTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is LoginEffect.NavigateToDashboard -> onLoginSuccess()
                is LoginEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LoginContent(state = state, onIntent = viewModel::handleIntent)
            if (state.isLoading) {
                LoadingScreen(message = "Authenticating...")
            }
        }
    }
}

@Composable
private fun LoginContent(
    state: LoginState,
    onIntent: (LoginIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.large
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AS",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Admin Login",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(48.dp))

        AnugrahaTextField(
            value = state.email,
            onValueChange = { onIntent(LoginIntent.EmailChanged(it)) },
            label = "Username",
            placeholder = "Enter your email",
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        )

        Spacer(modifier = Modifier.height(20.dp))

        AnugrahaPasswordTextField(
            value = state.password,
            onValueChange = { onIntent(LoginIntent.PasswordChanged(it)) },
            label = "Password",
            placeholder = "Enter your password",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(48.dp))

        state.error?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        AnugrahaPrimaryButton(
            text = "Submit",
            onClick = { onIntent(LoginIntent.Login) },
            isLoading = state.isLoading,
            enabled = state.email.isNotBlank() && state.password.isNotBlank()
        )
    }
}

@Preview
@Composable
private fun LoginPreview() {
    AnugrahaStaysTheme {
        LoginContent(
            state = LoginState(),
            onIntent = {}
        )
    }
}
