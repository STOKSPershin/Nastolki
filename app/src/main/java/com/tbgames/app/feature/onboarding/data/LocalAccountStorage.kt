package com.tbgames.app.feature.onboarding.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SavedAccount(
    val userId: String,
    val nickname: String,
    val avatarType: String,
    val avatarPresetId: Int,
    val avatarUrl: String?,
    val accessToken: String,
    val refreshToken: String
)

@Singleton
class LocalAccountStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tb_games_accounts", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    fun saveAccount(account: SavedAccount) {
        val accounts = getSavedAccounts().toMutableList()
        // Remove existing if any (update)
        accounts.removeAll { it.userId == account.userId }
        accounts.add(account)
        prefs.edit().putString("accounts", json.encodeToString(accounts)).apply()
    }

    fun getSavedAccounts(): List<SavedAccount> {
        val accountsStr = prefs.getString("accounts", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<SavedAccount>>(accountsStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun removeAccount(userId: String) {
        val accounts = getSavedAccounts().toMutableList()
        accounts.removeAll { it.userId == userId }
        prefs.edit().putString("accounts", json.encodeToString(accounts)).apply()
        
        if (getActiveAccountId() == userId) {
            clearActiveAccount()
        }
    }

    fun getActiveAccountId(): String? {
        return prefs.getString("active_account_id", null)
    }

    fun setActiveAccountId(userId: String) {
        prefs.edit().putString("active_account_id", userId).apply()
    }

    fun clearActiveAccount() {
        prefs.edit().remove("active_account_id").apply()
    }
}
