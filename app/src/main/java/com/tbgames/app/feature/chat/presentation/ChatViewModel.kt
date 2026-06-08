package com.tbgames.app.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tbgames.app.core.domain.model.ChatMessage
import com.tbgames.app.core.domain.model.PlayerProfile
import com.tbgames.app.feature.chat.data.ChatRepository
import com.tbgames.app.feature.onboarding.data.AuthRepository
import com.tbgames.app.feature.onboarding.data.ProfileRepository
import com.tbgames.app.core.common.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentMessage: String = "",
    val currentProfile: PlayerProfile? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        observeMessages()
        chatRepository.startPolling()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            when (val result = profileRepository.getProfile(userId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(currentProfile = result.data) }
                }
                is AppResult.Error -> {}
            }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatRepository.messages.collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
        viewModelScope.launch {
            chatRepository.isLoading.collect { loading ->
                _uiState.update { it.copy(isLoading = loading) }
            }
        }
    }

    fun onMessageChange(text: String) {
        _uiState.update { it.copy(currentMessage = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.currentMessage.trim()
        val profile = _uiState.value.currentProfile ?: return
        if (text.isEmpty()) return

        _uiState.update { it.copy(currentMessage = "") }

        viewModelScope.launch {
            chatRepository.sendMessage(
                ChatMessage(
                    userId = profile.id,
                    nickname = profile.nickname,
                    avatarType = profile.avatarType,
                    avatarPresetId = profile.avatarPresetId,
                    content = text
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.stopPolling()
    }
}
