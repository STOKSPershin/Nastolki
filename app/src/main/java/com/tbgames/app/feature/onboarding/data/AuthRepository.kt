package com.tbgames.app.feature.onboarding.data

import com.tbgames.app.core.common.AppResult
import com.tbgames.app.core.common.safeCall
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
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

    suspend fun restoreSession(accessToken: String, refreshToken: String): AppResult<Unit> = safeCall {
        supabase.auth.importAuthToken(accessToken, refreshToken)
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
