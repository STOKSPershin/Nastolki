package com.tbgames.app.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoomPlayer(
    @SerialName("room_id") val roomId: String,
    @SerialName("player_id") val playerId: String,
    @SerialName("is_host") val isHost: Boolean = false
)
