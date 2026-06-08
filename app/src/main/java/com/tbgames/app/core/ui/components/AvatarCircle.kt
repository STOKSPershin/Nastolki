package com.tbgames.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.tbgames.app.core.common.Constants
import com.tbgames.app.core.ui.theme.OnlineGreen
import com.tbgames.app.core.ui.theme.OfflineGray

@Composable
fun AvatarCircle(
    avatarType: String,
    avatarPresetId: Int,
    avatarUrl: String?,
    size: Dp = 48.dp,
    isOnline: Boolean? = null,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (avatarType == Constants.AVATAR_TYPE_CUSTOM && avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "\u0410\u0432\u0430\u0442\u0430\u0440",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(2.dp, borderColor, CircleShape)
            )
        } else {
            val colors = listOf(
                Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6),
                Color(0xFFFFD54F), Color(0xFFBA68C8), Color(0xFF4DB6AC),
                Color(0xFFFF8A65), Color(0xFFA1887F), Color(0xFF90A4AE),
                Color(0xFFF06292), Color(0xFFAED581), Color(0xFF4FC3F7),
                Color(0xFFFFB74D), Color(0xFF9575CD), Color(0xFF4DD0E1),
                Color(0xFFE6EE9C), Color(0xFFCE93D8), Color(0xFF80DEEA),
                Color(0xFFFFF176), Color(0xFFEF9A9A)
            )
            val bgColor = colors.getOrElse((avatarPresetId - 1).coerceIn(0, colors.size - 1)) { colors[0] }
            val animals = listOf(
                "\uD83D\uDC36", "\uD83D\uDC31", "\uD83D\uDC3B", "\uD83E\uDD8A", "\uD83D\uDC3C",
                "\uD83D\uDC28", "\uD83E\uDD81", "\uD83D\uDC38", "\uD83D\uDC27", "\uD83D\uDC26",
                "\uD83E\uDD84", "\uD83D\uDC22", "\uD83D\uDC2C", "\uD83E\uDD8B", "\uD83D\uDC1D",
                "\uD83E\uDD89", "\uD83D\uDC19", "\uD83E\uDD88", "\uD83D\uDC3A", "\uD83D\uDC30"
            )
            val emoji = animals.getOrElse((avatarPresetId - 1).coerceIn(0, animals.size - 1)) { animals[0] }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(2.dp, borderColor, CircleShape)
            ) {
                Text(
                    text = emoji,
                    fontSize = with(LocalDensity.current) { (size * 0.5f).toSp() }
                )
            }
        }

        if (isOnline != null) {
            Box(
                modifier = Modifier
                    .size(size * 0.25f)
                    .clip(CircleShape)
                    .background(if (isOnline) OnlineGreen else OfflineGray)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}
