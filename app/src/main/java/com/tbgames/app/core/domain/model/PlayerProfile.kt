package com.tbgames.app.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerProfile(
    val id: String = "",
    val nickname: String = "",
    @SerialName("avatar_type")
    val avatarType: String = "preset",
    @SerialName("avatar_preset_id")
    val avatarPresetId: Int = 1,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("device_id")
    val deviceId: String? = null,
    @SerialName("status_text")
    val statusText: String? = null,
    @SerialName("is_online")
    val isOnline: Boolean = false,
    @SerialName("current_room_id")
    val currentRoomId: String? = null,
    @SerialName("total_wins")
    val totalWins: Int = 0,
    @SerialName("total_losses")
    val totalLosses: Int = 0,
    @SerialName("total_games")
    val totalGames: Int = 0,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("status")
    val status: String? = null
)
