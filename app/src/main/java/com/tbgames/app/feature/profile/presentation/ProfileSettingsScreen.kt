package com.tbgames.app.feature.profile.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.tbgames.app.core.common.Constants
import com.tbgames.app.core.ui.components.AvatarCircle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    state: ProfileUiState,
    onBackClick: () -> Unit,
    onStartEditNickname: () -> Unit,
    onNicknameChange: (String) -> Unit,
    onSaveNickname: () -> Unit,
    onCancelEditNickname: () -> Unit,
    onAvatarPresetSelected: (Int) -> Unit,
    onCustomAvatarSelected: (android.net.Uri) -> Unit,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
    onThemeModeChange: (String) -> Unit,
    onShowLogoutDialog: () -> Unit,
    onHideLogoutDialog: () -> Unit,
    onLogout: () -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onCustomAvatarSelected(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Avatar section
            state.profile?.let { profile ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AvatarCircle(
                        avatarType = profile.avatarType,
                        avatarPresetId = profile.avatarPresetId,
                        avatarUrl = profile.avatarUrl,
                        size = 96.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Nickname
                    if (state.isEditingNickname) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = state.editingNickname,
                                onValueChange = onNicknameChange,
                                isError = state.nicknameError != null,
                                supportingText = {
                                    state.nicknameError?.let { Text(it) }
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onSaveNickname) {
                                Icon(Icons.Default.Check, contentDescription = "Сохранить")
                            }
                            IconButton(onClick = onCancelEditNickname) {
                                Icon(Icons.Default.Close, contentDescription = "Отмена")
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.nickname,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = onStartEditNickname) {
                                Icon(Icons.Default.Edit, contentDescription = "Изменить ник", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar grid
                Text(
                    text = "Сменить аватар",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items((1..Constants.PRESET_AVATARS_COUNT).toList()) { presetId ->
                        AvatarCircle(
                            avatarType = Constants.AVATAR_TYPE_PRESET,
                            avatarPresetId = presetId,
                            avatarUrl = null,
                            size = 56.dp,
                            borderColor = if (profile.avatarType == Constants.AVATAR_TYPE_PRESET && profile.avatarPresetId == presetId)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onAvatarPresetSelected(presetId) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Загрузить своё фото")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Statistics
                Text(
                    text = "Статистика",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("Побед", profile.totalWins.toString())
                        StatItem("Поражений", profile.totalLosses.toString())
                        StatItem("Всего игр", profile.totalGames.toString())
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Settings
                Text(
                    text = "Настройки",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Sound
                SettingRow(
                    title = "Звук",
                    checked = state.soundEnabled,
                    onCheckedChange = { onToggleSound() }
                )

                // Vibration
                SettingRow(
                    title = "Вибрация",
                    checked = state.vibrationEnabled,
                    onCheckedChange = { onToggleVibration() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Theme
                Text(
                    text = "Тема",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val themes = listOf(
                        Constants.ThemeMode.LIGHT to "Светлая",
                        Constants.ThemeMode.DARK to "Тёмная",
                        Constants.ThemeMode.SYSTEM to "Системная"
                    )
                    themes.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = state.themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, themes.size)
                        ) {
                            Text(label)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Logout button
                Button(
                    onClick = onShowLogoutDialog,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выйти из аккаунта")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Logout confirmation dialog
    if (state.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = onHideLogoutDialog,
            title = { Text("Выйти из аккаунта?") },
            text = {
                Text("Ваш профиль будет удалён. Вы сможете создать новый профиль при следующем входе.")
            },
            confirmButton = {
                TextButton(onClick = onLogout) {
                    Text("Да, выйти", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onHideLogoutDialog) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
