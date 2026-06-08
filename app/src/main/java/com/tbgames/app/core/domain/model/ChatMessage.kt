package com.tbgames.app.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    val nickname: String,
    @SerialName("avatar_type") val avatarType: String = "preset",
    @SerialName("avatar_preset_id") val avatarPresetId: Int = 1,
    val content: String,
    @SerialName("created_at") val createdAt: String = ""
)
