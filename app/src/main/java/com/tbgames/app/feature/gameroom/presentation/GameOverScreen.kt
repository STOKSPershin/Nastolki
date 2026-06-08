package com.tbgames.app.feature.gameroom.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tbgames.app.feature.gameroom.domain.model.FakeArtistGameState

@Composable
fun GameOverScreen(
    gameState: FakeArtistGameState,
    players: List<RoomPlayerInfo>,
    onExit: () -> Unit
) {
    val maxScore = gameState.scores.values.maxOrNull() ?: 0
    val winners = players.filter { gameState.scores[it.profile.id] == maxScore }

    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = "Победа",
            modifier = Modifier
                .size(120.dp)
                .scale(scale),
            tint = Color(0xFFFFD700) // Золотой
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Игра окончена!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val winnerText = if (winners.size == 1) {
            "Победитель: ${winners.first().profile.nickname}"
        } else {
            "Победители: ${winners.joinToString(", ") { it.profile.nickname }}"
        }
        
        Text(
            text = winnerText,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "со счетом $maxScore очк.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Вернуться в лобби", fontWeight = FontWeight.Bold)
        }
    }
}
