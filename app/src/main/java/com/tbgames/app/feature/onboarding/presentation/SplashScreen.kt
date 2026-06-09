package com.tbgames.app.feature.onboarding.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToLobby: () -> Unit,
    onNavigateToAccountSelection: () -> Unit,
    isLoggedIn: Boolean?,
    hasSavedAccounts: Boolean?
) {
    var startAnimation by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "splash_alpha"
    )

    val progressAnim by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 200),
        label = "progress_alpha"
    )

    LaunchedEffect(isLoggedIn, hasSavedAccounts) {
        startAnimation = true
        
        // Start progress animation
        while(progress < 0.9f && isLoggedIn == null) {
            delay(100)
            progress += 0.1f
        }

        // Wait for login status resolution
        if (isLoggedIn == true || (isLoggedIn == false && hasSavedAccounts != null)) {
            progress = 1f
            delay(500) // Small delay so user sees full progress bar
            if (isLoggedIn == true) {
                onNavigateToLobby()
            } else if (hasSavedAccounts == true) {
                onNavigateToAccountSelection()
            } else {
                onNavigateToOnboarding()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF161623)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(alphaAnim)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "НАСТОЛКИ",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = "&",
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFF523B),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = "НАСТОЙКИ",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Column(
                modifier = Modifier.fillMaxWidth(0.6f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "ЗАГРУЖАЕМ\nВЕСЕЛЬЕ...",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.5.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progressAnim },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFFF523B),
                    trackColor = Color(0xFF2A2A3D),
                )
            }
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}
