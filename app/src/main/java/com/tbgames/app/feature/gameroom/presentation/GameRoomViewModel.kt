package com.tbgames.app.feature.gameroom.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tbgames.app.core.domain.model.GameInfo
import com.tbgames.app.core.domain.model.OnlinePlayer
import com.tbgames.app.core.domain.model.PlayerProfile
import com.tbgames.app.core.domain.model.RoomPlayer
import com.tbgames.app.feature.onboarding.data.AuthRepository
import com.tbgames.app.feature.onboarding.data.ProfileRepository
import com.tbgames.app.core.common.AppResult
import com.tbgames.app.core.common.Constants
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class RoomPlayerInfo(
    val profile: PlayerProfile,
    val isHost: Boolean
)

data class GameRoomUiState(
    val roomId: String = "",
    val roomName: String = "",
    val gameInfo: GameInfo = GameInfo.FAKE_ARTIST,
    val players: List<RoomPlayerInfo> = emptyList(),
    val currentUserId: String = "",
    val isCurrentUserHost: Boolean = false,
    val isLoading: Boolean = true,
    val showRules: Boolean = false,
    val isRoomClosed: Boolean = false,
    val settings: com.tbgames.app.core.domain.model.RoomSettings = com.tbgames.app.core.domain.model.RoomSettings(),
    val error: String? = null
)

@HiltViewModel
class GameRoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameRoomUiState())
    val uiState: StateFlow<GameRoomUiState> = _uiState.asStateFlow()

    private var pollingJob: kotlinx.coroutines.Job? = null

    init {
        val roomId = savedStateHandle.get<String>("roomId") ?: ""
        _uiState.update { it.copy(roomId = roomId) }
        loadRoom(roomId)
    }

    private fun loadRoom(roomId: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        _uiState.update { it.copy(currentUserId = userId) }

        pollingJob?.cancel()
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    // Load room info
                    val rooms = supabase.postgrest["rooms"].select {
                        filter { eq("id", roomId) }
                    }.decodeList<com.tbgames.app.core.domain.model.GameRoom>()

                    val room = rooms.firstOrNull()
                    if (room == null) {
                        _uiState.update { it.copy(isRoomClosed = true, isLoading = false) }
                        break
                    }
                    _uiState.update { it.copy(roomName = room.name, settings = room.settings) }

                    // Load players in room
                    val roomPlayers = supabase.postgrest["room_players"].select {
                        filter { eq("room_id", roomId) }
                    }.decodeList<RoomPlayer>()

                    val playerInfos = roomPlayers.mapNotNull { rp ->
                        try {
                            val profiles = supabase.postgrest["profiles"].select {
                                filter { eq("id", rp.playerId) }
                            }.decodeList<PlayerProfile>()
                            profiles.firstOrNull()?.let { profile ->
                                RoomPlayerInfo(profile = profile, isHost = rp.isHost)
                            }
                        } catch (e: Exception) { null }
                    }

                    val isHost = roomPlayers.any { it.playerId == userId && it.isHost }

                    _uiState.update {
                        it.copy(
                            players = playerInfos,
                            isCurrentUserHost = isHost,
                            isLoading = false
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _uiState.update { it.copy(isLoading = false) }
                }
                delay(3000)
            }
        }
    }

    fun toggleRules() {
        _uiState.update { it.copy(showRules = !it.showRules) }
    }

    fun transferHost(toPlayerId: String) {
        val roomId = _uiState.value.roomId
        val currentUserId = _uiState.value.currentUserId
        if (!_uiState.value.isCurrentUserHost) return

        viewModelScope.launch {
            try {
                // Remove host from current user
                supabase.postgrest["room_players"].update(
                    mapOf("is_host" to false)
                ) {
                    filter {
                        eq("room_id", roomId)
                        eq("player_id", currentUserId)
                    }
                }
                // Set host on new player
                supabase.postgrest["room_players"].update(
                    mapOf("is_host" to true)
                ) {
                    filter {
                        eq("room_id", roomId)
                        eq("player_id", toPlayerId)
                    }
                }
                // Update room's host_id
                supabase.postgrest["rooms"].update(
                    mapOf("host_id" to toPlayerId)
                ) {
                    filter { eq("id", roomId) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Не удалось передать роль") }
            }
        }
    }

    fun updateSettings(settings: com.tbgames.app.core.domain.model.RoomSettings) {
        if (!_uiState.value.isCurrentUserHost) return
        val roomId = _uiState.value.roomId
        viewModelScope.launch {
            try {
                // Use explicit mapping to avoid json serialization issues with update
                supabase.postgrest["rooms"].update(
                    mapOf("settings" to settings)
                ) {
                    filter { eq("id", roomId) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun leaveRoom() {
        val roomId = _uiState.value.roomId
        val userId = _uiState.value.currentUserId
        val isHost = _uiState.value.isCurrentUserHost
        viewModelScope.launch {
            try {
                if (isHost) {
                    // Host leaves: delete the room itself.
                    // This cascades to room_players table, deleting everyone in the room.
                    supabase.postgrest["rooms"].delete {
                        filter { eq("id", roomId) }
                    }
                } else {
                    // Regular player leaves: delete their own room_players entry
                    supabase.postgrest["room_players"].delete {
                        filter {
                            eq("room_id", roomId)
                            eq("player_id", userId)
                        }
                    }
                    // Update current_players count in the room
                    val remaining = supabase.postgrest["room_players"].select {
                        filter { eq("room_id", roomId) }
                    }.decodeList<RoomPlayer>()
                    
                    supabase.postgrest["rooms"].update(
                        mapOf("current_players" to remaining.size)
                    ) {
                        filter { eq("id", roomId) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
