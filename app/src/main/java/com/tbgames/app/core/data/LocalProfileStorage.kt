package com.tbgames.app.core.data

import android.content.Context
import android.content.SharedPreferences
import com.tbgames.app.core.domain.model.PlayerProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalProfileStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tb_games_profile", Context.MODE_PRIVATE)

    /**
     * Returns a stable device UUID. Generated once on first launch
     * and persisted forever (survives app updates, only cleared on app data wipe).
     */
    fun getDeviceId(): String {
        val existing = prefs.getString("device_uuid", null)
        if (existing != null) return existing
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString("device_uuid", newId).apply()
        return newId
    }

    fun saveProfile(profile: PlayerProfile) {
        prefs.edit()
            .putString("user_id", profile.id)
            .putString("nickname", profile.nickname)
            .putString("avatar_type", profile.avatarType)
            .putInt("avatar_preset_id", profile.avatarPresetId)
            .putString("avatar_url", profile.avatarUrl)
            .putBoolean("has_profile", true)
            .apply()
    }

    fun hasProfile(): Boolean = prefs.getBoolean("has_profile", false)

    fun getProfile(): PlayerProfile? {
        if (!hasProfile()) return null
        return PlayerProfile(
            id = prefs.getString("user_id", "") ?: return null,
            nickname = prefs.getString("nickname", "") ?: return null,
            avatarType = prefs.getString("avatar_type", "preset") ?: "preset",
            avatarPresetId = prefs.getInt("avatar_preset_id", 1),
            avatarUrl = prefs.getString("avatar_url", null),
            deviceId = getDeviceId()
        )
    }

    fun getSavedNickname(): String? = prefs.getString("nickname", null)

    fun clear() {
        // Keep device_uuid on clear — it's the device fingerprint
        val deviceUuid = prefs.getString("device_uuid", null)
        prefs.edit().clear().apply()
        if (deviceUuid != null) {
            prefs.edit().putString("device_uuid", deviceUuid).apply()
        }
    }
}
