package com.tbgames.app.feature.profile.data

import com.tbgames.app.core.common.AppResult
import com.tbgames.app.core.common.Constants
import com.tbgames.app.core.common.safeCall
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.UploadData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvatarStorageRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): AppResult<String> = safeCall {
        val path = "$userId/avatar.jpg"
        val bucket = supabase.storage.from(Constants.AVATARS_BUCKET)
        bucket.upload(path, imageBytes) {
            upsert = true
        }
        bucket.publicUrl(path)
    }

    suspend fun deleteAvatar(userId: String): AppResult<Unit> = safeCall {
        val path = "$userId/avatar.jpg"
        supabase.storage.from(Constants.AVATARS_BUCKET).delete(path)
    }
}
