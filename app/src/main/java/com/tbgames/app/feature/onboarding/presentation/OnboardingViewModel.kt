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

import com.tbgames.app.feature.onboarding.data.LocalAccountStorage
import com.tbgames.app.feature.onboarding.data.SavedAccount

data class OnboardingUiState(
    val nickname: String = "",
    val nicknameError: String? = null,
    val selectedPresetId: Int = 1,
    val customAvatarUri: Uri? = null,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean? = null,
    val hasSavedAccounts: Boolean? = null,
    val savedAccounts: List<SavedAccount> = emptyList(),
    val isProfileCreated: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val localProfileStorage: LocalProfileStorage,
    private val avatarStorageRepository: AvatarStorageRepository,
    private val localAccountStorage: LocalAccountStorage,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        checkLoginStatus()
    }

    fun recheckLoginStatus() {
        // Reset state before checking to avoid stale data triggering navigation
        _uiState.update { 
            it.copy(
                isLoggedIn = null, 
                hasSavedAccounts = null,
                isLoading = true 
            ) 
        }
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            // Step 1: Check if Supabase session is alive and profile exists
            var loggedIn = authRepository.isLoggedIn()
            
            // If not logged in, but we have an active account, try to restore it implicitly!
            if (!loggedIn) {
                val activeAccountId = localAccountStorage.getActiveAccountId()
                if (activeAccountId != null) {
                    val savedAccounts = localAccountStorage.getSavedAccounts()
                    val activeAccount = savedAccounts.find { it.userId == activeAccountId }
                    if (activeAccount != null) {
                        val restoreResult = authRepository.restoreSession(activeAccount.accessToken, activeAccount.refreshToken)
                        if (restoreResult is AppResult.Success) {
                            loggedIn = true
                        }
                    }
                }
            }

            if (loggedIn) {
                val userId = authRepository.getCurrentUserId()
                if (userId != null) {
                    when (val result = profileRepository.getProfile(userId)) {
                        is AppResult.Success -> {
                            // Session valid, profile exists -> save locally and go
                            localProfileStorage.saveProfile(result.data)
                            localAccountStorage.setActiveAccountId(userId)
                            _uiState.update { it.copy(isLoggedIn = true) }
                            return@launch
                        }
                        is AppResult.Error -> {
                            // Session exists but profile gone -> fall through
                        }
                    }
                }
            }

            // Step 2: Session invalid or profile missing. Check for saved accounts.
            val savedAccounts = localAccountStorage.getSavedAccounts()
            if (savedAccounts.isNotEmpty()) {
                _uiState.update { 
                    it.copy(
                        isLoggedIn = false, 
                        hasSavedAccounts = true,
                        savedAccounts = savedAccounts,
                        isLoading = false
                    ) 
                }
                return@launch
            }

            // Step 3: No saved accounts and no session. This might be an existing user from before the multi-account update!
            // Let's try to recover their old profile using deviceId.
            val deviceId = localProfileStorage.getDeviceId()
            when (val deviceResult = profileRepository.getProfileByDeviceId(deviceId)) {
                is AppResult.Success -> {
                    val existingProfile = deviceResult.data
                    if (existingProfile != null) {
                        // We found an orphaned profile! Let's sign in anonymously and migrate it.
                        when (val authResult = authRepository.signInAnonymously()) {
                            is AppResult.Success -> {
                                val newUserId = authResult.data.id
                                val newAccessToken = authRepository.getCurrentAccessToken()
                                val newRefreshToken = authRepository.getCurrentRefreshToken()

                                // Delete the old profile row because we're moving it to a new auth user
                                profileRepository.deleteProfileByDeviceId(deviceId)

                                val restoredProfile = existingProfile.copy(
                                    id = newUserId,
                                    deviceId = java.util.UUID.randomUUID().toString() // Give it a new random deviceId to free up the physical deviceId
                                )

                                if (profileRepository.createProfile(restoredProfile) is AppResult.Success) {
                                    // Save to our new LocalAccountStorage
                                    if (newAccessToken != null && newRefreshToken != null) {
                                        localAccountStorage.saveAccount(
                                            com.tbgames.app.feature.onboarding.data.SavedAccount(
                                                userId = restoredProfile.id,
                                                nickname = restoredProfile.nickname,
                                                avatarType = restoredProfile.avatarType,
                                                avatarPresetId = restoredProfile.avatarPresetId,
                                                avatarUrl = restoredProfile.avatarUrl,
                                                accessToken = newAccessToken,
                                                refreshToken = newRefreshToken
                                            )
                                        )
                                    }
                                    localProfileStorage.saveProfile(restoredProfile)
                                    _uiState.update { it.copy(isLoggedIn = true) }
                                    return@launch
                                }
                            }
                            is AppResult.Error -> {}
                        }
                    }
                }
                is AppResult.Error -> {}
            }

            // Step 4: Nothing found. Show onboarding.
            _uiState.update { 
                it.copy(
                    isLoggedIn = false, 
                    hasSavedAccounts = false 
                ) 
            }
        }
    }

    fun deleteSavedAccount(account: SavedAccount) {
        localAccountStorage.removeAccount(account.userId)
        val savedAccounts = localAccountStorage.getSavedAccounts()
        _uiState.update { 
            it.copy(
                savedAccounts = savedAccounts,
                hasSavedAccounts = savedAccounts.isNotEmpty()
            ) 
        }
    }

    fun restoreAccount(account: SavedAccount) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val restoreResult = authRepository.restoreSession(account.accessToken, account.refreshToken)
            if (restoreResult is AppResult.Success) {
                // Fetch profile to make sure it exists
                when (val profileResult = profileRepository.getProfile(account.userId)) {
                    is AppResult.Success -> {
                        localAccountStorage.setActiveAccountId(account.userId)
                        localProfileStorage.saveProfile(profileResult.data)
                        _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                    }
                    is AppResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = "Профиль не найден") }
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Не удалось восстановить сессию") }
            }
        }
    }

    fun startNewAccountCreation() {
        _uiState.update { it.copy(hasSavedAccounts = false) }
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
                deviceId = java.util.UUID.randomUUID().toString()
            )

            when (profileRepository.createProfile(profile)) {
                is AppResult.Success -> {
                    val accessToken = authRepository.getCurrentAccessToken()
                    val refreshToken = authRepository.getCurrentRefreshToken()
                    if (accessToken != null && refreshToken != null) {
                        localAccountStorage.saveAccount(
                            com.tbgames.app.feature.onboarding.data.SavedAccount(
                                userId = profile.id,
                                nickname = profile.nickname,
                                avatarType = profile.avatarType,
                                avatarPresetId = profile.avatarPresetId,
                                avatarUrl = profile.avatarUrl,
                                accessToken = accessToken,
                                refreshToken = refreshToken
                            )
                        )
                        localAccountStorage.setActiveAccountId(profile.id)
                    }
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
