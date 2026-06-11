package com.tbgames.app.feature.onboarding.data

import com.tbgames.app.core.common.AppResult
import com.tbgames.app.core.common.safeCall
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val localAccountStorage: LocalAccountStorage
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        repositoryScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) {
                    val session = status.session
                    val userId = session.user?.id
                    if (userId != null) {
                        val savedAccounts = localAccountStorage.getSavedAccounts()
                        val existingAccount = savedAccounts.find { it.userId == userId }
                        if (existingAccount != null) {
                            val updatedAccount = existingAccount.copy(
                                accessToken = session.accessToken,
                                refreshToken = session.refreshToken
                            )
                            localAccountStorage.saveAccount(updatedAccount)
                        }
                    }
                }
            }
        }
    }

    suspend fun awaitInitialization() {
        supabase.auth.sessionStatus.filter { it !is SessionStatus.Initializing }.first()
    }
    suspend fun signInAnonymously(): AppResult<UserInfo> = safeCall {
        supabase.auth.signInAnonymously()
        supabase.auth.currentUserOrNull() ?: throw Exception("Failed to sign in")
    }

    suspend fun getCurrentUser(): UserInfo? {
        return supabase.auth.currentUserOrNull()
    }

    suspend fun isLoggedIn(): Boolean {
        return supabase.auth.currentUserOrNull() != null
    }

    fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }

    fun getCurrentAccessToken(): String? {
        return supabase.auth.currentSessionOrNull()?.accessToken
    }

    fun getCurrentRefreshToken(): String? {
        return supabase.auth.currentSessionOrNull()?.refreshToken
    }

    suspend fun restoreSession(accessToken: String, refreshToken: String): AppResult<Unit> {
        var lastException: Exception? = null
        var delayMs = 1000L
        repeat(3) { attempt ->
            try {
                supabase.auth.importAuthToken(accessToken, refreshToken)
                supabase.auth.retrieveUserForCurrentSession(updateSession = true)
                return AppResult.Success(Unit)
            } catch (e: Exception) {
                lastException = e
                if (attempt < 2) {
                    kotlinx.coroutines.delay(delayMs)
                    delayMs *= 2
                }
            }
        }
        return AppResult.Error(lastException?.message ?: "Failed to restore session")
    }

    suspend fun clearLocalSession() {
        supabase.auth.clearSession()
    }

    suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (_: Exception) {}
    }
}
