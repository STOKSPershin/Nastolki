package com.tbgames.app.feature.gameroom.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import com.tbgames.app.core.ui.components.AvatarCircle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameRoomScreen(
    state: GameRoomUiState,
    onBackClick: () -> Unit,
    onToggleRules: () -> Unit,
    onTransferHost: (String) -> Unit,
    onLeaveRoom: () -> Unit,
    onSettingsChange: (com.tbgames.app.core.domain.model.RoomSettings) -> Unit,
    onStartReadyCheck: () -> Unit,
    onToggleReady: (Boolean) -> Unit,
    onStartGame: () -> Unit,
    onRoundResult: (Int) -> Unit,
    onUpdateGameState: (com.tbgames.app.feature.gameroom.domain.model.FakeArtistGameState, String?) -> Unit
) {
    var showLeaveDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(state.isRoomClosed) {
        if (state.isRoomClosed) {
            android.widget.Toast.makeText(context, "Создатель комнаты покинул игру", android.widget.Toast.LENGTH_SHORT).show()
            onLeaveRoom()
            onBackClick()
        }
    }

    LaunchedEffect(state.roomStatus, state.players) {
        if (state.isCurrentUserHost && state.roomStatus == "ready_check" && state.players.isNotEmpty()) {
            if (state.players.all { it.isReady }) {
                onStartGame()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val isFakeArtistPlaying = state.roomStatus == "playing" && state.gameInfo.id == "fake_artist" && state.gameState != null
                        if (isFakeArtistPlaying) {
                            val fakeArtistGameState = try {
                                kotlinx.serialization.json.Json.decodeFromJsonElement(
                                    com.tbgames.app.feature.gameroom.domain.model.FakeArtistGameState.serializer(),
                                    state.gameState!!
                                )
                            } catch (e: Exception) { null }

                            if (fakeArtistGameState != null) {
                                Text(
                                    text = "Категория:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = fakeArtistGameState.category,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(text = "${state.gameInfo.emoji} ${state.gameInfo.name}", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                text = "${state.gameInfo.emoji} ${state.gameInfo.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.roomName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.isCurrentUserHost) {
                            showLeaveDialog = true
                        } else {
                            onLeaveRoom()
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleRules) {
                        Icon(Icons.Default.Info, contentDescription = "Правила")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            if ((state.roomStatus == "playing" || state.roomStatus == "game_over") && state.gameInfo.id == "fake_artist" && state.gameState != null) {
                val fakeArtistGameState = try {
                    kotlinx.serialization.json.Json.decodeFromJsonElement(
                        com.tbgames.app.feature.gameroom.domain.model.FakeArtistGameState.serializer(),
                        state.gameState
                    )
                } catch (e: Exception) { null }

                if (fakeArtistGameState != null) {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        if (state.roomStatus == "game_over") {
                            com.tbgames.app.feature.gameroom.presentation.GameOverScreen(
                                gameState = fakeArtistGameState,
                                players = state.players,
                                onExit = {
                                    onLeaveRoom()
                                    onBackClick()
                                }
                            )
                        } else {
                            FakeArtistGameContent(
                                gameState = fakeArtistGameState,
                                settings = state.settings,
                                players = state.players,
                                currentUserId = state.currentUserId,
                                isHost = state.isCurrentUserHost,
                                onRoundResult = onRoundResult,
                                onUpdateState = onUpdateGameState
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                ) {
                Spacer(modifier = Modifier.height(8.dp))

                if (state.gameInfo.id == "fake_artist") {
                    RoomSettingsSection(
                        settings = state.settings,
                        isHost = state.isCurrentUserHost,
                        onSettingsChange = onSettingsChange
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Player count
                Text(
                    text = "\uD83D\uDC65 ${state.players.size} игроков в комнате",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Players list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(state.players, key = { it.profile.id }) { playerInfo ->
                        RoomPlayerCard(
                            playerInfo = playerInfo,
                            isCurrentUserHost = state.isCurrentUserHost,
                            isCurrentUser = playerInfo.profile.id == state.currentUserId,
                            onTransferHost = { onTransferHost(playerInfo.profile.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (state.isCurrentUserHost && state.roomStatus == "waiting") {
                    Button(
                        onClick = onStartReadyCheck,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Начать игру", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Rules button
                OutlinedButton(
                    onClick = onToggleRules,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Правила игры")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Leave room button
                Button(
                    onClick = {
                        if (state.isCurrentUserHost) {
                            showLeaveDialog = true
                        } else {
                            onLeaveRoom()
                            onBackClick()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Покинуть комнату")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

    // Rules dialog
        if (state.showRules) {
            AlertDialog(
                onDismissRequest = onToggleRules,
                properties = DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier.fillMaxWidth(0.95f),
                title = {
                    Text("${state.gameInfo.emoji} Правила: ${state.gameInfo.name}")
                },
                text = {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                    ) {
                        Text(state.gameInfo.rules)
                    }
                },
                confirmButton = {
                    TextButton(onClick = onToggleRules) {
                        Text("Понятно")
                    }
                }
            )
        }

        if (showLeaveDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveDialog = false },
                title = { Text("Выйти из комнаты?") },
                text = { Text("Вы являетесь ведущим. Если вы выйдете, комната будет удалена, а все игроки отключены.") },
                confirmButton = {
                    TextButton(onClick = {
                        showLeaveDialog = false
                        onLeaveRoom()
                        onBackClick()
                    }) { Text("Выйти", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveDialog = false }) { Text("Отмена") }
                }
            )
        }

        if (state.roomStatus == "ready_check") {
            ReadyCheckDialog(
                players = state.players,
                currentUserId = state.currentUserId,
                onToggleReady = onToggleReady
            )
        }
}

@Composable
fun ReadyCheckDialog(
    players: List<RoomPlayerInfo>,
    currentUserId: String,
    onToggleReady: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss manually */ },
        title = { Text("Готовность к игре", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                players.forEach { player ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    if (player.isReady) Color.Green 
                                    else Color.Gray,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = player.profile.nickname,
                            color = if (player.isReady) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = if (player.profile.id == currentUserId) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            val me = players.find { it.profile.id == currentUserId }
            if (me != null) {
                Button(
                    onClick = { onToggleReady(!me.isReady) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (me.isReady) Color.Gray else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (me.isReady) "Не готов" else "Готов!")
                }
            }
        },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}

@Composable
private fun RoomPlayerCard(
    playerInfo: RoomPlayerInfo,
    isCurrentUserHost: Boolean,
    isCurrentUser: Boolean,
    onTransferHost: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (playerInfo.isHost)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarCircle(
                avatarType = playerInfo.profile.avatarType,
                avatarPresetId = playerInfo.profile.avatarPresetId,
                avatarUrl = playerInfo.profile.avatarUrl,
                size = 44.dp,
                isOnline = true
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = playerInfo.profile.nickname,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(вы)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (playerInfo.isHost) {
                    Text(
                        text = "⭐ Ведущий",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Transfer host button - only shown to the current host, and not on themselves
            if (isCurrentUserHost && !playerInfo.isHost) {
                IconButton(
                    onClick = onTransferHost,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.StarOutline,
                        contentDescription = "Передать роль ведущего",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (playerInfo.isHost) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Ведущий",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun RoomSettingsSection(
    settings: com.tbgames.app.core.domain.model.RoomSettings,
    isHost: Boolean,
    onSettingsChange: (com.tbgames.app.core.domain.model.RoomSettings) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Победа по:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.RadioButton(
                        selected = settings.victoryType == "rounds",
                        onClick = { if (isHost) onSettingsChange(settings.copy(victoryType = "rounds", victoryValue = 1)) },
                        enabled = isHost,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(" Раунды", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.width(12.dp))

                    androidx.compose.material3.RadioButton(
                        selected = settings.victoryType == "points",
                        onClick = { if (isHost) onSettingsChange(settings.copy(victoryType = "points", victoryValue = 1)) },
                        enabled = isHost,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(" Очки", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (settings.victoryType == "rounds") "Кол-во раундов:" else "Кол-во очков:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Box {
                    var expanded by remember { mutableStateOf(false) }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(enabled = isHost) { expanded = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = settings.victoryValue.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = "Выбрать",
                            tint = if (isHost) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        (1..50).forEach { value ->
                            DropdownMenuItem(
                                text = { Text(value.toString()) },
                                onClick = {
                                    onSettingsChange(settings.copy(victoryValue = value))
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
