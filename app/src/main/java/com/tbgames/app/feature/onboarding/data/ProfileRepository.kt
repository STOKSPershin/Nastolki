package com.tbgames.app.feature.onboarding.data

import com.tbgames.app.core.common.AppResult
import com.tbgames.app.core.common.safeCall
import com.tbgames.app.core.domain.model.PlayerProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val table = "profiles"

    suspend fun createProfile(profile: PlayerProfile): AppResult<PlayerProfile> = safeCall {
        supabase.postgrest[table].insert(profile)
        profile
    }

    suspend fun getProfile(userId: String): AppResult<PlayerProfile> = safeCall {
        supabase.postgrest[table]
            .select { filter { eq("id", userId) } }
            .decodeSingle<PlayerProfile>()
    }

    suspend fun getProfileByDeviceId(deviceId: String): AppResult<PlayerProfile?> = safeCall {
        val results = supabase.postgrest[table]
            .select { filter { eq("device_id", deviceId) } }
            .decodeList<PlayerProfile>()
        results.firstOrNull()
    }

    suspend fun updateProfile(profile: PlayerProfile): AppResult<PlayerProfile> = safeCall {
        supabase.postgrest[table]
            .update(profile) { filter { eq("id", profile.id) } }
        profile
    }

    suspend fun checkNicknameAvailable(nickname: String): AppResult<Boolean> = safeCall {
        val results = supabase.postgrest[table]
            .select { filter { eq("nickname", nickname) } }
            .decodeList<PlayerProfile>()
        results.isEmpty()
    }

    suspend fun deleteProfileByDeviceId(deviceId: String): AppResult<Unit> = safeCall {
        supabase.postgrest[table].delete {
            filter { eq("device_id", deviceId) }
        }
    }

    suspend fun deleteProfile(userId: String): AppResult<Unit> = safeCall {
        supabase.postgrest[table].delete {
            filter { eq("id", userId) }
        }
    }
}
