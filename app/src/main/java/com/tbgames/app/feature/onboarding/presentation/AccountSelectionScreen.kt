package com.tbgames.app.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbgames.app.core.ui.components.AvatarCircle
import com.tbgames.app.core.ui.theme.InGameOrange
import com.tbgames.app.feature.onboarding.data.SavedAccount

@Composable
fun AccountSelectionScreen(
    accounts: List<SavedAccount>,
    isLoading: Boolean,
    error: String?,
    onAccountClick: (SavedAccount) -> Unit,
    onCreateNewAccountClick: () -> Unit,
    onDeleteAccountClick: (SavedAccount) -> Unit
) {
    var accountToDelete by remember { mutableStateOf<SavedAccount?>(null) }

    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Удалить аккаунт?") },
            text = { Text("Вы уверены, что хотите удалить аккаунт ${accountToDelete?.nickname}? Восстановить его будет невозможно.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        accountToDelete?.let { onDeleteAccountClick(it) }
                        accountToDelete = null
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "С возвращением!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Выберите аккаунт для входа",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.LightGray
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                CircularProgressIndicator(color = InGameOrange)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(accounts) { account ->
                        AccountCard(
                            account = account,
                            onClick = { onAccountClick(account) },
                            onDeleteClick = { accountToDelete = account }
                        )
                    }

                    item {
                        CreateNewAccountCard(onClick = onCreateNewAccountClick)
                    }
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AccountCard(
    account: SavedAccount,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarCircle(
                avatarType = account.avatarType,
                avatarPresetId = account.avatarPresetId,
                avatarUrl = account.avatarUrl,
                size = 56.dp,
                isOnline = true
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = account.nickname,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                ),
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить аккаунт",
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun CreateNewAccountCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = InGameOrange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Создать новый аккаунт",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = InGameOrange
                )
            )
        }
    }
}
