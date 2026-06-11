package com.tbgames.app.feature.lobby.data

import kotlinx.serialization.Serializable

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.tbgames.app.core.common.Constants
import com.tbgames.app.core.domain.model.OnlinePlayer
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class StatusUpdate(val status: String)

@Singleton
class PresenceRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val _onlinePlayers = MutableStateFlow<List<OnlinePlayer>>(emptyList())
    val onlinePlayers: Flow<List<OnlinePlayer>> = _onlinePlayers.asStateFlow()

    private val _hasInitialData = MutableStateFlow(false)
    val hasInitialData: Flow<Boolean> = _hasInitialData.asStateFlow()

    private var pollingJob: kotlinx.coroutines.Job? = null
    private var currentPlayer: OnlinePlayer? = null

    suspend fun joinLobby(player: OnlinePlayer) {
        currentPlayer = player
        pollingJob?.cancel()
        _hasInitialData.value = false

        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    try {
                        supabase.postgrest["profiles"].update(
                            StatusUpdate(currentPlayer?.status ?: player.status)
                        ) {
                            filter { eq("id", player.id) }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val allProfiles = supabase.postgrest["profiles"]
                        .select { filter { neq("status", "offline") } }
                        .decodeList<com.tbgames.app.core.domain.model.PlayerProfile>()

                    val latestServerTime = allProfiles.mapNotNull {
                        parseSupabaseTime(it.updatedAt).takeIf { t -> t > 0L }
                    }.maxOrNull() ?: System.currentTimeMillis()

                    val players = allProfiles
                        .filter { it.status != null && it.status != "offline" }
                        .filter { profile ->
                            val updatedAt = parseSupabaseTime(profile.updatedAt)
                            if (updatedAt > 0L) {
                                (latestServerTime - updatedAt) <= 40000L
                            } else {
                                false
                            }
                        }
                        .map { p ->
                            OnlinePlayer(
                                id = p.id,
                                nickname = p.nickname,
                                avatarType = p.avatarType,
                                avatarPresetId = p.avatarPresetId,
                                avatarUrl = p.avatarUrl,
                                status = if (p.id == player.id) (currentPlayer?.status ?: player.status) else (p.status ?: Constants.PlayerStatus.IN_LOBBY)
                            )
                        }
                    _onlinePlayers.value = players
                    _hasInitialData.value = true

                } catch (e: Exception) {
                    e.printStackTrace()
                    _hasInitialData.value = true // Stop showing loading even on error
                }
                delay(8000)
            }
        }
    }

    suspend fun updatePresence(status: String) {
        currentPlayer = currentPlayer?.copy(status = status)
        try {
            currentPlayer?.let { player ->
                supabase.postgrest["profiles"].update(
                    StatusUpdate(status)
                ) {
                    filter { eq("id", player.id) }
                }
            }
        } catch (e: Exception) {}
    }

    suspend fun leaveLobby() {
        pollingJob?.cancel()
        pollingJob = null
        try {
            currentPlayer?.let { player ->
                supabase.postgrest["profiles"].update(
                    StatusUpdate("offline")
                ) {
                    filter { eq("id", player.id) }
                }
            }
        } catch (e: Exception) {}
        currentPlayer = null
        _onlinePlayers.value = emptyList()
        _hasInitialData.value = false
    }

    fun updatePlayersList(players: List<OnlinePlayer>) {
        _onlinePlayers.value = players
    }

    private fun parseSupabaseTime(timeString: String?): Long {
        if (timeString == null) return 0L
        try {
            var clean = timeString.replace(" ", "T")
            if (clean.endsWith("+00")) {
                clean = clean.substringBeforeLast("+00") + "Z"
            } else if (clean.endsWith("+00:00")) {
                clean = clean.substringBeforeLast("+00:00") + "Z"
            }
            if (!clean.contains("Z") && !clean.contains("+") && clean.lastIndexOf("-") < 10) {
                clean += "Z"
            }
            if (clean.contains(".")) {
                val dotIndex = clean.indexOf(".")
                var suffix = ""
                if (clean.endsWith("Z")) {
                    suffix = "Z"
                } else {
                    val plusIndex = clean.indexOf("+", dotIndex)
                    val minusIndex = clean.indexOf("-", dotIndex)
                    if (plusIndex > 0) {
                        suffix = clean.substring(plusIndex)
                    } else if (minusIndex > 0) {
                        suffix = clean.substring(minusIndex)
                    }
                }
                clean = clean.substring(0, dotIndex) + suffix
            }
            return java.time.Instant.parse(clean).toEpochMilli()
        } catch (e: Exception) {
            return 0L
        }
    }
}
