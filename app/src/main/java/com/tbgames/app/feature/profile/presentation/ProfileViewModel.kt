package com.tbgames.app.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tbgames.app.core.common.AppResult
import com.tbgames.app.core.common.Constants
import com.tbgames.app.core.data.LocalProfileStorage
import com.tbgames.app.core.data.PreferencesManager
import com.tbgames.app.core.domain.model.PlayerProfile
import com.tbgames.app.feature.onboarding.data.AuthRepository
import com.tbgames.app.feature.onboarding.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: PlayerProfile? = null,
    val isLoading: Boolean = false,
    val editingNickname: String = "",
    val isEditingNickname: Boolean = false,
    val nicknameError: String? = null,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val themeMode: String = Constants.ThemeMode.SYSTEM,
    val error: String? = null,
    val saved: Boolean = false,
    val loggedOut: Boolean = false,
    val showLogoutDialog: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val preferencesManager: PreferencesManager,
    private val localProfileStorage: LocalProfileStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        observePreferences()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            when (val result = profileRepository.getProfile(userId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(profile = result.data) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesManager.soundEnabled.collect { enabled ->
                _uiState.update { it.copy(soundEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            preferencesManager.vibrationEnabled.collect { enabled ->
                _uiState.update { it.copy(vibrationEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            preferencesManager.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
    }

    fun startEditNickname() {
        _uiState.update { it.copy(isEditingNickname = true, editingNickname = it.profile?.nickname ?: "") }
    }

    fun onEditNicknameChange(value: String) {
        val error = when {
            value.length < Constants.MIN_NICKNAME_LENGTH && value.isNotEmpty() -> "Минимум ${Constants.MIN_NICKNAME_LENGTH} символа"
            value.length > Constants.MAX_NICKNAME_LENGTH -> "Максимум ${Constants.MAX_NICKNAME_LENGTH} символов"
            value.isNotEmpty() && !Constants.NICKNAME_REGEX.matches(value) -> "Только буквы, цифры и _"
            else -> null
        }
        _uiState.update { it.copy(editingNickname = value, nicknameError = error) }
    }

    fun saveNickname() {
        val profile = _uiState.value.profile ?: return
        val newNickname = _uiState.value.editingNickname
        if (newNickname.length < Constants.MIN_NICKNAME_LENGTH) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val updated = profile.copy(nickname = newNickname)
            when (profileRepository.updateProfile(updated)) {
                is AppResult.Success -> {
                    localProfileStorage.saveProfile(updated)
                    _uiState.update { it.copy(profile = updated, isEditingNickname = false, isLoading = false) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = "Не удалось сохранить") }
                }
            }
        }
    }

    fun cancelEditNickname() {
        _uiState.update { it.copy(isEditingNickname = false, nicknameError = null) }
    }

    fun onAvatarPresetSelected(presetId: Int) {
        val profile = _uiState.value.profile ?: return
        viewModelScope.launch {
            val updated = profile.copy(
                avatarType = Constants.AVATAR_TYPE_PRESET,
                avatarPresetId = presetId,
                avatarUrl = null
            )
            when (profileRepository.updateProfile(updated)) {
                is AppResult.Success -> {
                    localProfileStorage.saveProfile(updated)
                    _uiState.update { it.copy(profile = updated) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(error = "Не удалось обновить аватар") }
                }
            }
        }
    }

    fun toggleSound() {
        viewModelScope.launch {
            preferencesManager.setSoundEnabled(!_uiState.value.soundEnabled)
        }
    }

    fun toggleVibration() {
        viewModelScope.launch {
            preferencesManager.setVibrationEnabled(!_uiState.value.vibrationEnabled)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    fun showLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = true) }
    }

    fun hideLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = false) }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(showLogoutDialog = false, isLoading = true) }

            // Delete profile from DB
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                profileRepository.deleteProfile(userId)
            }

            // Also delete by device_id to be thorough
            val deviceId = localProfileStorage.getDeviceId()
            profileRepository.deleteProfileByDeviceId(deviceId)

            // Clear local storage (keeps device UUID)
            localProfileStorage.clear()

            // Sign out from Supabase
            authRepository.signOut()

            _uiState.update { it.copy(isLoading = false, loggedOut = true) }
        }
    }
}
