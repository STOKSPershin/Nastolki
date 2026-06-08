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
    val isHost: Boolean,
    val isReady: Boolean = false
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
    val roomStatus: String = "waiting",
    val gameState: kotlinx.serialization.json.JsonElement? = null,
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
                    _uiState.update { it.copy(roomName = room.name, settings = room.settings, roomStatus = room.status, gameState = room.gameState) }

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
                                RoomPlayerInfo(profile = profile, isHost = rp.isHost, isReady = rp.isReady)
                            }
                        } catch (e: Exception) { null }
                    }

                    // Heartbeat for current user
                    try {
                        supabase.postgrest["profiles"].update(
                            mapOf("updated_at" to java.time.Instant.now().toString())
                        ) { filter { eq("id", userId) } }
                    } catch (e: Exception) {}

                    val now = java.time.Instant.now()
                    val activePlayerInfos = playerInfos.filter { info ->
                        try {
                            val updatedAt = java.time.Instant.parse(info.profile.updatedAt)
                            java.time.Duration.between(updatedAt, now).seconds < 15
                        } catch (e: Exception) { false }
                    }

                    val isHostMissing = activePlayerInfos.isNotEmpty() && activePlayerInfos.none { it.isHost }
                    if (isHostMissing) {
                        val newHostId = activePlayerInfos.minByOrNull { it.profile.id }?.profile?.id
                        if (newHostId == userId) {
                            claimHost(userId, roomId)
                        }
                    }

                    val isHost = activePlayerInfos.any { it.profile.id == userId && it.isHost }

                    _uiState.update {
                        it.copy(
                            players = activePlayerInfos,
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

    fun startReadyCheck() {
        if (!_uiState.value.isCurrentUserHost) return
        viewModelScope.launch {
            try {
                supabase.postgrest["rooms"].update(
                    mapOf("status" to "ready_check")
                ) { filter { eq("id", _uiState.value.roomId) } }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleReady(isReady: Boolean) {
        viewModelScope.launch {
            try {
                supabase.postgrest["room_players"].update(
                    mapOf("is_ready" to isReady)
                ) {
                    filter {
                        eq("room_id", _uiState.value.roomId)
                        eq("player_id", _uiState.value.currentUserId)
                    }
                }
                
                // If I am host, check if everyone is ready to auto-start.
                // Wait, maybe we just let the host click 'Start' or do it automatically.
                // The user said: "когда все игроки в комнате нажали что готовы игра стартует"
                // So if everyone is ready, transition to playing.
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startGame() {
        if (!_uiState.value.isCurrentUserHost) return
        viewModelScope.launch {
            try {
                val players = _uiState.value.players
                if (players.isEmpty()) return@launch

                val spyId = players.random().profile.id
                val (category, word) = com.tbgames.app.feature.gameroom.domain.model.FakeArtistDictionary.getRandomWord()
                
                val initialState = com.tbgames.app.feature.gameroom.domain.model.FakeArtistGameState(
                    category = category,
                    word = word,
                    spyId = spyId,
                    round = 1,
                    scores = players.associate { it.profile.id to 0 }
                )
                
                val jsonState = kotlinx.serialization.json.Json.encodeToJsonElement(
                    com.tbgames.app.feature.gameroom.domain.model.FakeArtistGameState.serializer(), 
                    initialState
                )

                @kotlinx.serialization.Serializable
                data class RoomStartUpdate(
                    val status: String,
                    @kotlinx.serialization.SerialName("game_state")
                    val gameState: kotlinx.serialization.json.JsonElement
                )

                supabase.postgrest["rooms"].update(
                    RoomStartUpdate(
                        status = "playing",
                        gameState = jsonState
                    )
                ) { filter { eq("id", _uiState.value.roomId) } }
            } catch (e: Exception) {
                e.printStackTrace()
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

    private fun claimHost(userId: String, roomId: String) {
        viewModelScope.launch {
            try {
                // Clear all current hosts for this room
                supabase.postgrest["room_players"].update(
                    mapOf("is_host" to false)
                ) { filter { eq("room_id", roomId) } }
                
                // Set myself as host
                supabase.postgrest["room_players"].update(
                    mapOf("is_host" to true)
                ) {
                    filter {
                        eq("room_id", roomId)
                        eq("player_id", userId)
                    }
                }
                
                // Update room
                supabase.postgrest["rooms"].update(
                    mapOf("host_id" to userId)
                ) { filter { eq("id", roomId) } }
            } catch (e: Exception) {}
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

    fun handleRoundResult(spyPoints: Int) {
        if (!_uiState.value.isCurrentUserHost) return
        
        viewModelScope.launch {
            try {
                val currentState = _uiState.value.gameState ?: return@launch
                
                val fakeArtistGameState = kotlinx.serialization.json.Json.decodeFromJsonElement(
                    com.tbgames.app.feature.gameroom.domain.model.FakeArtistGameState.serializer(),
                    currentState
                )
                
                val players = _uiState.value.players
                val newScores = fakeArtistGameState.scores.toMutableMap()
                
                if (spyPoints > 0) {
                    val currentSpyScore = newScores[fakeArtistGameState.spyId] ?: 0
                    newScores[fakeArtistGameState.spyId] = currentSpyScore + spyPoints
                } else {
                    players.forEach { player ->
                        if (player.profile.id != fakeArtistGameState.spyId) {
                            val currentScore = newScores[player.profile.id] ?: 0
                            newScores[player.profile.id] = currentScore + 1
                        }
                    }
                }
                
                val settings = _uiState.value.settings
                val isVictory = if (settings.victoryType == "rounds") {
                    fakeArtistGameState.round >= settings.victoryValue
                } else {
                    newScores.values.any { it >= settings.victoryValue }
                }
                
                if (isVictory) {
                    // Update final scores and set status to game_over
                    val finalState = fakeArtistGameState.copy(scores = newScores)
                    val jsonState = kotlinx.serialization.json.Json.encodeToJsonElement(
                        com.tbgames.app.feature.gameroom.domain.model.FakeArtistGameState.serializer(),
                        finalState
                    )
                    @kotlinx.serialization.Serializable
                    data class GameOverUpdate(
                        val status: String,
                        @kotlinx.serialization.SerialName("game_state")
                        val gameState: kotlinx.serialization.json.JsonElement
                    )
                    
                    supabase.postgrest["rooms"].update(
                        GameOverUpdate(
                            status = "game_over",
                            gameState = jsonState
                        )
                    ) { filter { eq("id", _uiState.value.roomId) } }
                } else {
                    // Next round
                    val spyId = players.random().profile.id
                    val (category, word) = com.tbgames.app.feature.gameroom.domain.model.FakeArtistDictionary.getRandomWord()
                    
                    val nextState = fakeArtistGameState.copy(
                        round = fakeArtistGameState.round + 1,
                        spyId = spyId,
                        category = category,
                        word = word,
                        scores = newScores
                    )
                    
                    val jsonState = kotlinx.serialization.json.Json.encodeToJsonElement(
                        com.tbgames.app.feature.gameroom.domain.model.FakeArtistGameState.serializer(),
                        nextState
                    )
                    
                    @kotlinx.serialization.Serializable
                    data class NextRoundUpdate(
                        @kotlinx.serialization.SerialName("game_state")
                        val gameState: kotlinx.serialization.json.JsonElement
                    )

                    supabase.postgrest["rooms"].update(
                        NextRoundUpdate(gameState = jsonState)
                    ) { filter { eq("id", _uiState.value.roomId) } }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateGameState(
        newState: com.tbgames.app.feature.gameroom.domain.model.FakeArtistGameState,
        status: String? = null
    ) {
        if (!_uiState.value.isCurrentUserHost) return
        
        viewModelScope.launch {
            try {
                val jsonState = kotlinx.serialization.json.Json.encodeToJsonElement(
                    com.tbgames.app.feature.gameroom.domain.model.FakeArtistGameState.serializer(),
                    newState
                )
                
                @kotlinx.serialization.Serializable
                data class GameStateUpdate(
                    val status: String,
                    @kotlinx.serialization.SerialName("game_state")
                    val gameState: kotlinx.serialization.json.JsonElement
                )
                
                supabase.postgrest["rooms"].update(
                    GameStateUpdate(
                        status = status ?: _uiState.value.roomStatus,
                        gameState = jsonState
                    )
                ) { filter { eq("id", _uiState.value.roomId) } }
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
