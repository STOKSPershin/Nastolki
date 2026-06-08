package com.tbgames.app.feature.gameroom.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FakeArtistGameState(
    val category: String = "",
    val word: String = "",
    @SerialName("spy_id")
    val spyId: String = "",
    val round: Int = 1,
    val scores: Map<String, Int> = emptyMap() // Map of playerId to score
)
