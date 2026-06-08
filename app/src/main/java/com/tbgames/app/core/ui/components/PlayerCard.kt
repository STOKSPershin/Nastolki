package com.tbgames.app.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tbgames.app.core.common.Constants
import com.tbgames.app.core.domain.model.OnlinePlayer
import com.tbgames.app.core.ui.theme.InGameOrange
import com.tbgames.app.core.ui.theme.InRoomBlue
import com.tbgames.app.core.ui.theme.OnlineGreen

@Composable
fun PlayerCard(
    player: OnlinePlayer,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarCircle(
                avatarType = player.avatarType,
                avatarPresetId = player.avatarPresetId,
                avatarUrl = player.avatarUrl,
                size = 48.dp,
                isOnline = true
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.nickname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                val statusText = when (player.status) {
                    Constants.PlayerStatus.IN_LOBBY -> "\u0412 \u043B\u043E\u0431\u0431\u0438"
                    Constants.PlayerStatus.IN_ROOM -> "\u0412 \u043A\u043E\u043C\u043D\u0430\u0442\u0435"
                    Constants.PlayerStatus.IN_GAME -> "\u0412 \u0438\u0433\u0440\u0435"
                    else -> "\u0412 \u043B\u043E\u0431\u0431\u0438"
                }
                val statusColor = when (player.status) {
                    Constants.PlayerStatus.IN_LOBBY -> OnlineGreen
                    Constants.PlayerStatus.IN_ROOM -> InRoomBlue
                    Constants.PlayerStatus.IN_GAME -> InGameOrange
                    else -> OnlineGreen
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }
        }
    }
}
