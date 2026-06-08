package com.tbgames.app.feature.onboarding.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tbgames.app.core.ui.components.LoadingButton

@Composable
fun NicknameScreen(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    nicknameError: String?,
    isLoading: Boolean,
    onNext: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "\u041A\u0430\u043A \u0442\u0435\u0431\u044F \u0437\u043E\u0432\u0443\u0442?",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\u041F\u0440\u0438\u0434\u0443\u043C\u0430\u0439 \u043D\u0438\u043A\u043D\u0435\u0439\u043C \u0434\u043B\u044F \u0438\u0433\u0440\u044B",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                label = { Text("\u041D\u0438\u043A\u043D\u0435\u0439\u043C") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                isError = nicknameError != null,
                supportingText = {
                    if (nicknameError != null) {
                        Text(nicknameError, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("3-16 \u0441\u0438\u043C\u0432\u043E\u043B\u043E\u0432: \u0431\u0443\u043A\u0432\u044B, \u0446\u0438\u0444\u0440\u044B, _")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onNext()
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            LoadingButton(
                text = "\u0414\u0430\u043B\u0435\u0435",
                isLoading = isLoading,
                onClick = onNext,
                enabled = nickname.length >= 3 && nicknameError == null,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
