package com.tbgames.app.feature.chat.data

import com.tbgames.app.core.domain.model.ChatMessage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: Flow<Boolean> = _isLoading.asStateFlow()

    private var pollingJob: kotlinx.coroutines.Job? = null

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val msgs = supabase.postgrest["messages"]
                        .select {
                            order("created_at", Order.DESCENDING)
                            limit(100)
                        }
                        .decodeList<ChatMessage>()
                        .reversed()
                    _messages.value = msgs
                    _isLoading.value = false
                } catch (e: Exception) {
                    e.printStackTrace()
                    _isLoading.value = false
                }
                delay(3000)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    suspend fun sendMessage(message: ChatMessage) {
        try {
            supabase.postgrest["messages"].insert(message)
            // Immediately refresh
            val msgs = supabase.postgrest["messages"]
                .select {
                    order("created_at", Order.DESCENDING)
                    limit(100)
                }
                .decodeList<ChatMessage>()
                .reversed()
            _messages.value = msgs
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
