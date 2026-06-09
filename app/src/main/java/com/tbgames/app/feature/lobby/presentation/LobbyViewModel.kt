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
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        observeOnlinePlayers()
        loadRooms()
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

    fun loadRooms() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRooms = true) }
            when (val result = roomRepository.getRooms()) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(rooms = result.data, isLoadingRooms = false) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoadingRooms = false, error = result.message) }
                }
            }
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
                    loadRooms()
                    // Navigate AFTER join is complete
                    _uiState.update { it.copy(navigateToRoomId = roomId) }
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
            roomRepository.joinRoom(roomId, profile.id)
            loadRooms()
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            presenceRepository.leaveLobby()
        }
    }
}
