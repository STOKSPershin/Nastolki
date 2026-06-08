package com.tbgames.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.tbgames.app.core.common.Constants
import com.tbgames.app.core.data.PreferencesManager
import com.tbgames.app.core.ui.theme.TBGamesTheme
import com.tbgames.app.navigation.TBGamesNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by preferencesManager.themeMode.collectAsState(initial = Constants.ThemeMode.SYSTEM)

            val darkTheme = when (themeMode) {
                Constants.ThemeMode.DARK -> true
                Constants.ThemeMode.LIGHT -> false
                else -> null // System default
            }

            TBGamesTheme(
                darkTheme = darkTheme ?: androidx.compose.foundation.isSystemInDarkTheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    TBGamesNavGraph(navController = navController)
                }
            }
        }
    }
}
