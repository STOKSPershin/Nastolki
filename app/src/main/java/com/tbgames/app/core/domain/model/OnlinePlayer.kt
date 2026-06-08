package com.tbgames.app.core.domain.model

data class OnlinePlayer(
    val id: String,
    val nickname: String,
    val avatarType: String,
    val avatarPresetId: Int,
    val avatarUrl: String?,
    val status: String
)
