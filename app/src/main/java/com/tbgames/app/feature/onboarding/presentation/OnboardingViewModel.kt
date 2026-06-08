package com.tbgames.app.feature.onboarding.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tbgames.app.core.common.AppResult
import com.tbgames.app.core.common.Constants
import com.tbgames.app.core.data.LocalProfileStorage
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
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.tbgames.app.feature.profile.data.AvatarStorageRepository
import com.tbgames.app.core.utils.ImageHelper

data class OnboardingUiState(
    val nickname: String = "",
    val nicknameError: String? = null,
    val selectedPresetId: Int = 1,
    val customAvatarUri: Uri? = null,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean? = null,
    val isProfileCreated: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val localProfileStorage: LocalProfileStorage,
    private val avatarStorageRepository: AvatarStorageRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            val deviceId = localProfileStorage.getDeviceId()

            // Step 1: Check if Supabase session is alive and profile exists
            val loggedIn = authRepository.isLoggedIn()
            if (loggedIn) {
                val userId = authRepository.getCurrentUserId()
                if (userId != null) {
                    when (val result = profileRepository.getProfile(userId)) {
                        is AppResult.Success -> {
                            // Session valid, profile exists — save locally and go
                            localProfileStorage.saveProfile(result.data)
                            _uiState.update { it.copy(isLoggedIn = true) }
                            return@launch
                        }
                        is AppResult.Error -> {
                            // Session exists but profile gone — fall through
                        }
                    }
                }
            }

            // Step 2: No valid session — check if this device had a profile before
            // Sign in anonymously to get a new auth user
            val authResult = authRepository.signInAnonymously()
            if (authResult is AppResult.Error) {
                _uiState.update { it.copy(isLoggedIn = false) }
                return@launch
            }
            val newUserId = (authResult as AppResult.Success).data.id

            // Look up existing profile by device_id
            when (val deviceResult = profileRepository.getProfileByDeviceId(deviceId)) {
                is AppResult.Success -> {
                    val existingProfile = deviceResult.data
                    if (existingProfile != null) {
                        // Device had a profile — delete old row, create new one with new user_id
                        profileRepository.deleteProfileByDeviceId(deviceId)

                        val restoredProfile = existingProfile.copy(
                            id = newUserId,
                            deviceId = deviceId
                        )
                        when (profileRepository.createProfile(restoredProfile)) {
                            is AppResult.Success -> {
                                localProfileStorage.saveProfile(restoredProfile)
                                _uiState.update { it.copy(isLoggedIn = true) }
                                return@launch
                            }
                            is AppResult.Error -> {
                                // Failed to recreate — show onboarding
                            }
                        }
                    }
                }
                is AppResult.Error -> {
                    // Can't check — fall through
                }
            }

            // Step 3: Also check local storage as last resort
            val localProfile = localProfileStorage.getProfile()
            if (localProfile != null) {
                profileRepository.deleteProfileByDeviceId(deviceId)

                val restoredProfile = localProfile.copy(
                    id = newUserId,
                    deviceId = deviceId
                )
                when (profileRepository.createProfile(restoredProfile)) {
                    is AppResult.Success -> {
                        localProfileStorage.saveProfile(restoredProfile)
                        _uiState.update { it.copy(isLoggedIn = true) }
                        return@launch
                    }
                    is AppResult.Error -> { /* fall through */ }
                }
            }

            // Step 4: No profile anywhere — show onboarding
            _uiState.update { it.copy(isLoggedIn = false) }
        }
    }

    fun onNicknameChange(value: String) {
        val error = when {
            value.length < Constants.MIN_NICKNAME_LENGTH && value.isNotEmpty() ->
                "Минимум ${Constants.MIN_NICKNAME_LENGTH} символа"
            value.length > Constants.MAX_NICKNAME_LENGTH ->
                "Максимум ${Constants.MAX_NICKNAME_LENGTH} символов"
            value.isNotEmpty() && !Constants.NICKNAME_REGEX.matches(value) ->
                "Только буквы, цифры и подчёркивание"
            else -> null
        }
        _uiState.update { it.copy(nickname = value, nicknameError = error) }
    }

    fun checkNicknameAndProceed() {
        val nickname = _uiState.value.nickname
        if (nickname.length < Constants.MIN_NICKNAME_LENGTH) {
            _uiState.update { it.copy(nicknameError = "Минимум ${Constants.MIN_NICKNAME_LENGTH} символа") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = profileRepository.checkNicknameAvailable(nickname)) {
                is AppResult.Success -> {
                    if (result.data) {
                        _uiState.update { it.copy(isLoading = false, nicknameError = null) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, nicknameError = "Этот никнейм уже занят") }
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, nicknameError = null) }
                }
            }
        }
    }

    fun onPresetSelected(presetId: Int) {
        _uiState.update { it.copy(selectedPresetId = presetId, customAvatarUri = null) }
    }

    fun onCustomAvatarSelected(uri: Uri) {
        _uiState.update { it.copy(customAvatarUri = uri) }
    }

    fun createProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val deviceId = localProfileStorage.getDeviceId()

            // Ensure we have an auth session
            var userId = authRepository.getCurrentUserId()
            if (userId == null) {
                when (val authResult = authRepository.signInAnonymously()) {
                    is AppResult.Success -> { userId = authResult.data.id }
                    is AppResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = "Ошибка авторизации") }
                        return@launch
                    }
                }
            }

            var avatarUrl: String? = null
            if (_uiState.value.customAvatarUri != null) {
                val uri = _uiState.value.customAvatarUri!!
                val bytes = ImageHelper.getCompressedAvatarBytes(context, uri)
                if (bytes != null) {
                    when (val uploadResult = avatarStorageRepository.uploadAvatar(userId, bytes)) {
                        is AppResult.Success -> {
                            avatarUrl = uploadResult.data
                        }
                        is AppResult.Error -> {
                            // Proceed without custom avatar if upload fails, or return error
                            // We'll just fall back to preset if it fails
                            _uiState.update { it.copy(error = "Не удалось загрузить фото. Использован стандартный аватар.") }
                        }
                    }
                }
            }

            val finalAvatarType = if (avatarUrl != null) Constants.AVATAR_TYPE_CUSTOM else Constants.AVATAR_TYPE_PRESET

            val profile = PlayerProfile(
                id = userId,
                nickname = _uiState.value.nickname,
                avatarType = finalAvatarType,
                avatarPresetId = _uiState.value.selectedPresetId,
                avatarUrl = avatarUrl,
                deviceId = deviceId
            )

            when (profileRepository.createProfile(profile)) {
                is AppResult.Success -> {
                    localProfileStorage.saveProfile(profile)
                    _uiState.update { it.copy(isLoading = false, isProfileCreated = true) }
                }
                is AppResult.Error -> {
                    // If creating the profile fails, the auth session might be stale or invalid (e.g., account deleted on server)
                    viewModelScope.launch {
                        authRepository.signOut()
                    }
                    _uiState.update { it.copy(isLoading = false, error = "Не удалось создать профиль. Возможно, аккаунт был сброшен. Попробуйте нажать Готово еще раз.") }
                }
            }
        }
    }
}
