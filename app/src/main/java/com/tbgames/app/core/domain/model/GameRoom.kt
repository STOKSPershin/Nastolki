package com.tbgames.app.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoomSettings(
    @SerialName("victory_type")
    val victoryType: String = "rounds", // "rounds" or "points"
    @SerialName("victory_value")
    val victoryValue: Int = 10
)

@Serializable
data class GameRoom(
    val id: String = "",
    val name: String = "",
    @SerialName("game_type")
    val gameType: String = "default",
    @SerialName("host_id")
    val hostId: String = "",
    val status: String = "waiting",
    @SerialName("max_players")
    val maxPlayers: Int = 4,
    @SerialName("current_players")
    val currentPlayers: Int = 0,
    val settings: RoomSettings = RoomSettings(),
    @SerialName("game_state")
    val gameState: kotlinx.serialization.json.JsonElement? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)
