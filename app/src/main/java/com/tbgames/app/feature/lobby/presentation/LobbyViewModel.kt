package com.tbgames.app.feature.lobby.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tbgames.app.core.common.AppResult
import com.tbgames.app.core.common.Constants
import com.tbgames.app.core.domain.model.GameInfo
import com.tbgames.app.core.domain.model.GameRoom
import com.tbgames.app.core.domain.model.OnlinePlayer
import com.tbgames.app.core.domain.model.PlayerProfile
import com.tbgames.app.feature.lobby.data.PresenceRepository
import com.tbgames.app.feature.lobby.data.RoomRepository
import com.tbgames.app.feature.onboarding.data.AuthRepository
import com.tbgames.app.feature.onboarding.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.tbgames.app.core.domain.model.RoomPlayer
import java.util.UUID
import javax.inject.Inject

data class LobbyUiState(
    val currentProfile: PlayerProfile? = null,
    val onlinePlayers: List<OnlinePlayer> = emptyList(),
    val rooms: List<GameRoom> = emptyList(),
    val isLoadingPlayers: Boolean = true,
    val isLoadingRooms: Boolean = false,
    val showGameSelectDialog: Boolean = false,
    val navigateToRoomId: String? = null,
    val error: String? = null
)

@HiltViewModel
class LobbyViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val presenceRepository: PresenceRepository,
    private val roomRepository: RoomRepository,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    private var roomsPollingJob: kotlinx.coroutines.Job? = null

    init {
        loadProfile()
        observeOnlinePlayers()
        startRoomsPolling()
    }

    private fun startRoomsPolling() {
        roomsPollingJob?.cancel()
        roomsPollingJob = viewModelScope.launch {
            while (isActive) {
                loadRoomsInternal()
                delay(8000)
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            when (val result = profileRepository.getProfile(userId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(currentProfile = result.data) }
                    val profile = result.data
                    presenceRepository.joinLobby(
                        OnlinePlayer(
                            id = profile.id,
                            nickname = profile.nickname,
                            avatarType = profile.avatarType,
                            avatarPresetId = profile.avatarPresetId,
                            avatarUrl = profile.avatarUrl,
                            status = Constants.PlayerStatus.IN_LOBBY
                        )
                    )
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    private fun observeOnlinePlayers() {
        viewModelScope.launch {
            presenceRepository.onlinePlayers.collect { players ->
                _uiState.update { it.copy(onlinePlayers = players) }
            }
        }
        viewModelScope.launch {
            presenceRepository.hasInitialData.collect { initialized ->
                if (initialized) {
                    _uiState.update { it.copy(isLoadingPlayers = false) }
                }
            }
        }
    }

    fun enterLobby() {
        viewModelScope.launch {
            try {
                presenceRepository.updatePresence(Constants.PlayerStatus.IN_LOBBY)
            } catch (_: Exception) {}
            loadRoomsInternal()
        }
    }

    fun loadRooms() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRooms = true) }
            loadRoomsInternal()
        }
    }

    private suspend fun loadRoomsInternal() {
        when (val result = roomRepository.getRooms()) {
            is AppResult.Success -> {
                val rooms = result.data
                val roomIds = rooms.map { it.id }
                if (roomIds.isNotEmpty()) {
                    try {
                        val roomPlayers = supabase.postgrest["room_players"].select {
                            filter { isIn("room_id", roomIds) }
                        }.decodeList<RoomPlayer>()

                        val playersByRoom = roomPlayers.groupBy { it.roomId }
                        val hostIds = rooms.map { it.hostId }
                        val allPlayerIds = (roomPlayers.map { it.playerId } + hostIds).distinct()

                        var latestServerTime = System.currentTimeMillis()

                        if (allPlayerIds.isNotEmpty()) {
                            val profiles = supabase.postgrest["profiles"].select {
                                filter { isIn("id", allPlayerIds) }
                            }.decodeList<PlayerProfile>()

                            latestServerTime = profiles.mapNotNull { p ->
                                parseSupabaseTime(p.updatedAt).takeIf { it > 0L }
                            }.maxOrNull() ?: System.currentTimeMillis()

                            val activePlayerIds = profiles.filter { profile ->
                                val updatedAt = parseSupabaseTime(profile.updatedAt)
                                val isTrulyOffline = profile.status == "offline" || profile.status == null
                                val isStale = updatedAt > 0L && (latestServerTime - updatedAt) > 60_000L
                                // Player is active if: not offline, not stale, and has valid updated_at
                                !isTrulyOffline && !isStale && updatedAt > 0L
                            }.map { it.id }.toSet()

                            val validRooms = mutableListOf<GameRoom>()
                            
                            rooms.forEach { room ->
                                val players = playersByRoom[room.id] ?: emptyList()
                                
                                 if (players.isEmpty()) {
                                     val hostProfile = profiles.find { it.id == room.hostId }
                                     val isHostOffline = hostProfile == null || 
                                                         hostProfile.status == "offline" || 
                                                         hostProfile.status == null || 
                                                         (parseSupabaseTime(hostProfile.updatedAt) > 0L && 
                                                          (latestServerTime - parseSupabaseTime(hostProfile.updatedAt)) > 60_000L)
                                     if (isHostOffline) {
                                         try {
                                             supabase.postgrest["rooms"].delete { filter { eq("id", room.id) } }
                                         } catch (_: Exception) {}
                                     }
                                     return@forEach
                                 }

                                val activePlayers = if (room.status == "playing") players else players.filter { it.playerId in activePlayerIds }
                                // Only remove players who are truly stale (offline + not updating for 60s+)
                                val stalePlayerIds = if (room.status == "playing") emptyList() else players.map { it.playerId }.filter { playerId ->
                                    val profile = profiles.find { it.id == playerId }
                                    if (profile == null) return@filter true // no profile = stale
                                    val isTrulyOffline = profile.status == "offline" || profile.status == null
                                    val updatedAt = parseSupabaseTime(profile.updatedAt)
                                    val isStale = updatedAt > 0L && (latestServerTime - updatedAt) > 60_000L
                                    isTrulyOffline && isStale // only remove if BOTH offline AND stale
                                }

                                if (stalePlayerIds.isNotEmpty() && room.status == "waiting") {
                                    stalePlayerIds.forEach { staleId ->
                                        try {
                                            supabase.postgrest["room_players"].delete {
                                                filter {
                                                    eq("room_id", room.id)
                                                    eq("player_id", staleId)
                                                }
                                            }
                                        } catch (_: Exception) {}
                                    }
                                }

                                 if (activePlayers.isEmpty()) {
                                     val hostProfile = profiles.find { it.id == room.hostId }
                                     val isHostOffline = hostProfile == null || 
                                                         hostProfile.status == "offline" || 
                                                         hostProfile.status == null || 
                                                         (parseSupabaseTime(hostProfile.updatedAt) > 0L && 
                                                          (latestServerTime - parseSupabaseTime(hostProfile.updatedAt)) > 60_000L)
                                     if (isHostOffline) {
                                         try {
                                             supabase.postgrest["rooms"].delete { filter { eq("id", room.id) } }
                                         } catch (_: Exception) {}
                                     } else {
                                         // Host is still alive, show room even if no "active" players by strict filter
                                         validRooms.add(room.copy(currentPlayers = players.size - stalePlayerIds.size))
                                     }
                                 } else {
                                    val hasHost = activePlayers.any { it.isHost }
                                    if (!hasHost) {
                                        val newHost = activePlayers.first()
                                        try {
                                            supabase.postgrest["room_players"].update(
                                                mapOf("is_host" to true)
                                            ) {
                                                filter {
                                                    eq("room_id", room.id)
                                                    eq("player_id", newHost.playerId)
                                                }
                                            }
                                            supabase.postgrest["rooms"].update(
                                                mapOf("host_id" to newHost.playerId, "current_players" to activePlayers.size)
                                            ) { filter { eq("id", room.id) } }
                                        } catch (_: Exception) {}
                                    } else {
                                        if (room.currentPlayers != activePlayers.size) {
                                            try {
                                                supabase.postgrest["rooms"].update(
                                                    mapOf("current_players" to activePlayers.size)
                                                ) { filter { eq("id", room.id) } }
                                            } catch (_: Exception) {}
                                        }
                                    }
                                    validRooms.add(room.copy(currentPlayers = activePlayers.size))
                                }
                            }
                            _uiState.update { it.copy(rooms = validRooms, isLoadingRooms = false) }
                        } else {
                            rooms.forEach { room ->
                                val createdTime = parseSupabaseTime(room.createdAt)
                                val isRecentlyCreated = createdTime > 0L && (latestServerTime - createdTime) <= 15_000L
                                if (!isRecentlyCreated) {
                                    try {
                                        supabase.postgrest["rooms"].delete { filter { eq("id", room.id) } }
                                    } catch (_: Exception) {}
                                }
                            }
                            _uiState.update { it.copy(rooms = emptyList(), isLoadingRooms = false) }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _uiState.update { it.copy(rooms = rooms, isLoadingRooms = false) }
                    }
                } else {
                    _uiState.update { it.copy(rooms = emptyList(), isLoadingRooms = false) }
                }
            }
            is AppResult.Error -> {
                _uiState.update { it.copy(isLoadingRooms = false, error = result.message) }
            }
        }
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
        } catch (_: Exception) {
            return 0L
        }
    }

    fun showGameSelectDialog() {
        _uiState.update { it.copy(showGameSelectDialog = true) }
    }

    fun hideGameSelectDialog() {
        _uiState.update { it.copy(showGameSelectDialog = false) }
    }

    fun createRoom(gameInfo: GameInfo) {
        val profile = _uiState.value.currentProfile ?: return

        val roomId = UUID.randomUUID().toString()
        val room = GameRoom(
            id = roomId,
            name = "${gameInfo.name} — ${profile.nickname}",
            gameType = gameInfo.id,
            hostId = profile.id,
            maxPlayers = gameInfo.maxPlayers,
            currentPlayers = 1
        )

        viewModelScope.launch {
            when (roomRepository.createRoom(room)) {
                is AppResult.Success -> {
                    roomRepository.joinRoom(roomId, profile.id, isHost = true)
                    hideGameSelectDialog()
                    _uiState.update { it.copy(navigateToRoomId = roomId) }
                    // Update presence and rooms in background, don't block navigation
                    try {
                        presenceRepository.updatePresence(Constants.PlayerStatus.IN_ROOM)
                    } catch (_: Exception) {}
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(error = "Не удалось создать комнату") }
                }
            }
        }
    }

    fun clearNavigation() {
        _uiState.update { it.copy(navigateToRoomId = null) }
    }

    fun joinRoom(roomId: String) {
        val profile = _uiState.value.currentProfile ?: return
        viewModelScope.launch {
            when (val result = roomRepository.joinRoom(roomId, profile.id)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(navigateToRoomId = roomId) }
                    // Update presence in background, don't block navigation
                    try {
                        presenceRepository.updatePresence(Constants.PlayerStatus.IN_ROOM)
                    } catch (_: Exception) {}
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(error = "Не удалось войти в комнату: ${result.message}") }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            presenceRepository.leaveLobby()
        }
    }
}
