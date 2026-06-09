package com.tbgames.app.feature.gameroom.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tbgames.app.core.domain.model.RoomSettings
import com.tbgames.app.feature.gameroom.domain.model.PasswordGameState
import kotlinx.coroutines.delay

@Composable
fun PasswordGameContent(
    gameState: PasswordGameState,
    settings: RoomSettings,
    players: List<RoomPlayerInfo>,
    currentUserId: String,
    onSubmitWord: (String) -> Unit,
    onAwardPoints: (Int) -> Unit
) {
    val currentPair = gameState.pairs.getOrNull(gameState.currentRoundIndex) ?: return
    
    val role = when (currentUserId) {
        currentPair.thinkerId -> "Загадывающий"
        currentPair.guesserId -> "Отгадывающий"
        else -> "Подсказывающий"
    }
    
    var timeRemaining by remember { mutableStateOf(0L) }
    
    LaunchedEffect(gameState.timerEndTime, gameState.status) {
        if (gameState.status == "input_word" && gameState.timerEndTime != null) {
            while (true) {
                val remaining = gameState.timerEndTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    timeRemaining = 0
                    if (currentUserId == currentPair.thinkerId) {
                        onAwardPoints(0)
                    }
                    break
                }
                timeRemaining = remaining / 1000
                delay(1000)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Role & Round Header
        Text(
            text = "Роль: $role  •  Раунд ${gameState.currentRoundIndex + 1} из ${gameState.pairs.size}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Statistics Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Игрок", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Роль", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Очки", fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.5f))
                }
                
                players.forEach { p ->
                    val pRole = when (p.profile.id) {
                        currentPair.thinkerId -> "Загадывающий"
                        currentPair.guesserId -> "Отгадывающий"
                        else -> "Подсказывающий"
                    }
                    val score = gameState.scores[p.profile.id] ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(p.profile.nickname, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(pRole, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text(score.toString(), textAlign = TextAlign.End, modifier = Modifier.weight(0.5f))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (gameState.status == "input_word") {
            if (role == "Загадывающий") {
                var inputWord by remember { mutableStateOf("") }
                
                Text("Придумайте слово-пароль:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = inputWord,
                    onValueChange = { inputWord = it },
                    placeholder = { Text("Только существительное") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { if (inputWord.isNotBlank()) onSubmitWord(inputWord.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = inputWord.isNotBlank()
                ) {
                    Text("Загадать")
                }
            } else {
                Text(
                    text = "Ожидаем загадывающего...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Осталось времени: $timeRemaining сек",
                style = MaterialTheme.typography.titleMedium,
                color = if (timeRemaining < 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        } else if (gameState.status == "playing_round") {
            if (role == "Отгадывающий") {
                Text(
                    text = "Внимательно слушайте подсказки от других игроков!",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                // Word Spoiler
                var isWordVisible by remember { mutableStateOf(false) }
                
                Text("Загаданное слово:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isWordVisible = true
                                    tryAwaitRelease()
                                    isWordVisible = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isWordVisible) {
                        Text(
                            text = gameState.word ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Text(
                            text = "Удерживайте, чтобы посмотреть",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                
                if (role == "Загадывающий") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Начисление баллов:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var useDropdown by remember { mutableStateOf(false) }
                    var pointsCounter by remember { mutableStateOf(0) }
                    var expanded by remember { mutableStateOf(false) }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = !useDropdown, onCheckedChange = { useDropdown = !it })
                        Text("Счетчик")
                        Spacer(modifier = Modifier.width(16.dp))
                        Checkbox(checked = useDropdown, onCheckedChange = { useDropdown = it })
                        Text("Список")
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    if (useDropdown) {
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth().height(45.dp)
                            ) {
                                Text("Выбрано баллов: $pointsCounter")
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                (0..20).forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("$p баллов") },
                                        onClick = { pointsCounter = p; expanded = false }
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { if (pointsCounter > 0) pointsCounter-- },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).size(40.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Меньше")
                            }
                            Spacer(modifier = Modifier.width(24.dp))
                            Text(
                                text = pointsCounter.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(24.dp))
                            IconButton(
                                onClick = { pointsCounter++ },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).size(40.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Больше")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onAwardPoints(pointsCounter) },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Начислить $pointsCounter баллов", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
