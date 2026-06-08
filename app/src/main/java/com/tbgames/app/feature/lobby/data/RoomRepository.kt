package com.tbgames.app.feature.lobby.data

import com.tbgames.app.core.common.AppResult
import com.tbgames.app.core.common.safeCall
import com.tbgames.app.core.domain.model.GameRoom
import com.tbgames.app.core.domain.model.RoomPlayer
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getRooms(): AppResult<List<GameRoom>> = safeCall {
        supabase.postgrest["rooms"]
            .select { filter { neq("status", "finished") } }
            .decodeList<GameRoom>()
    }

    suspend fun createRoom(room: GameRoom): AppResult<GameRoom> = safeCall {
        supabase.postgrest["rooms"]
            .insert(room) { select() }
            .decodeSingle<GameRoom>()
    }

    suspend fun joinRoom(roomId: String, playerId: String, isHost: Boolean = false): AppResult<Unit> = safeCall {
        supabase.postgrest["room_players"].insert(
            RoomPlayer(roomId = roomId, playerId = playerId, isHost = isHost)
        )
        // Update current_players count
        val players = supabase.postgrest["room_players"]
            .select { filter { eq("room_id", roomId) } }
            .decodeList<RoomPlayer>()
        supabase.postgrest["rooms"].update(
            mapOf("current_players" to players.size)
        ) { filter { eq("id", roomId) } }
    }

    suspend fun leaveRoom(roomId: String, playerId: String): AppResult<Unit> = safeCall {
        supabase.postgrest["room_players"].delete {
            filter {
                eq("room_id", roomId)
                eq("player_id", playerId)
            }
        }
        val remaining = supabase.postgrest["room_players"]
            .select { filter { eq("room_id", roomId) } }
            .decodeList<RoomPlayer>()
        if (remaining.isEmpty()) {
            supabase.postgrest["rooms"].delete {
                filter { eq("id", roomId) }
            }
        } else {
            supabase.postgrest["rooms"].update(
                mapOf("current_players" to remaining.size)
            ) { filter { eq("id", roomId) } }
        }
    }

    suspend fun updateRoomStatus(roomId: String, status: String): AppResult<Unit> = safeCall {
        supabase.postgrest["rooms"].update(
            mapOf("status" to status)
        ) { filter { eq("id", roomId) } }
    }

    suspend fun updatePlayerReady(roomId: String, playerId: String, isReady: Boolean): AppResult<Unit> = safeCall {
        supabase.postgrest["room_players"].update(
            mapOf("is_ready" to isReady)
        ) { filter { 
            eq("room_id", roomId)
            eq("player_id", playerId)
        } }
    }

    suspend fun updateGameState(roomId: String, gameState: kotlinx.serialization.json.JsonElement): AppResult<Unit> = safeCall {
        supabase.postgrest["rooms"].update(
            mapOf("game_state" to gameState)
        ) { filter { eq("id", roomId) } }
    }
}
