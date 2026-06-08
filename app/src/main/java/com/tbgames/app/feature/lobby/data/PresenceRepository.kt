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
data class StatusUpdate(
    val status: String,
    @kotlinx.serialization.SerialName("updated_at")
    val updatedAt: String = java.time.Instant.now().toString()
)

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
                            StatusUpdate(player.status)
                        ) {
                            filter { eq("id", player.id) }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val allProfiles = supabase.postgrest["profiles"]
                        .select { filter { neq("status", "offline") } }
                        .decodeList<com.tbgames.app.core.domain.model.PlayerProfile>()

                    val now = java.time.Instant.now()
                    val players = allProfiles
                        .filter { it.status != null && it.status != "offline" }
                        .filter { profile ->
                            try {
                                val updatedAt = java.time.Instant.parse(profile.updatedAt)
                                java.time.Duration.between(updatedAt, now).seconds < 15
                            } catch (e: Exception) {
                                false // If no updatedAt or invalid format, assume offline
                            }
                        }
                        .map { p ->
                            OnlinePlayer(
                                id = p.id,
                                nickname = p.nickname,
                                avatarType = p.avatarType,
                                avatarPresetId = p.avatarPresetId,
                                avatarUrl = p.avatarUrl,
                                status = if (p.id == player.id) player.status else (p.status ?: Constants.PlayerStatus.IN_LOBBY)
                            )
                        }
                    _onlinePlayers.value = players
                    _hasInitialData.value = true

                } catch (e: Exception) {
                    e.printStackTrace()
                    _hasInitialData.value = true // Stop showing loading even on error
                }
                delay(5000)
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
}
