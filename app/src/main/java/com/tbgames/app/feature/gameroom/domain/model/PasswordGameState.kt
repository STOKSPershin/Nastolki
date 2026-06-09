package com.tbgames.app.feature.gameroom.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PasswordRoundPair(
    val thinkerId: String,
    val guesserId: String
)

@Serializable
data class PasswordGameState(
    val pairs: List<PasswordRoundPair> = emptyList(),
    val currentRoundIndex: Int = 0,
    val scores: Map<String, Int> = emptyMap(),
    val status: String = "input_word", // "input_word", "playing_round", "game_over"
    val word: String? = null,
    val timerEndTime: Long? = null
)
