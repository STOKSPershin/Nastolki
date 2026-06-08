package com.tbgames.app.feature.gameroom.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tbgames.app.core.domain.model.RoomSettings
import com.tbgames.app.feature.gameroom.domain.model.FakeArtistGameState

@Composable
fun FakeArtistGameContent(
    gameState: FakeArtistGameState,
    settings: RoomSettings,
    players: List<RoomPlayerInfo>,
    currentUserId: String,
    isHost: Boolean,
    onRoundResult: (Int) -> Unit
) {
    var showWord by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Категория
        Text(
            text = "Категория",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = gameState.category,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Загаданное слово
        Text(
            text = "Загаданное слово:",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))

        val isSpy = gameState.spyId == currentUserId
        val displayedWord = if (isSpy) "Вы шпион!" else gameState.word

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (showWord) MaterialTheme.colorScheme.surfaceVariant 
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (showWord) {
                    Text(
                        text = displayedWord,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSpy) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = "Нажмите на глаз",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                showWord = true
                                tryAwaitRelease()
                                showWord = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Показать слово",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Статистика
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val victoryCondition = if (settings.victoryType == "rounds") "Игра до ${settings.victoryValue} раундов" else "Игра до ${settings.victoryValue} очков"
                Text(
                    text = "Раунд ${gameState.round} • $victoryCondition",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val sortedPlayers = players.sortedByDescending { gameState.scores[it.profile.id] ?: 0 }
                    items(sortedPlayers, key = { it.profile.id }) { player ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = player.profile.nickname,
                                fontWeight = if (player.profile.id == currentUserId) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "${gameState.scores[player.profile.id] ?: 0} очк.",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (isHost) {
            Button(
                onClick = { showResultDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Результат раунда", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showResultDialog && isHost) {
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            title = { Text("Очки Шпиона") },
            text = { Text("Сколько очков заработал Шпион в этом раунде? (Если 0, то все художники получат по 1 очку).") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onRoundResult(0); showResultDialog = false }) { Text("0") }
                    Button(onClick = { onRoundResult(1); showResultDialog = false }) { Text("1") }
                    Button(onClick = { onRoundResult(2); showResultDialog = false }) { Text("2") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showResultDialog = false }) { Text("Отмена") }
            }
        )
    }
}
