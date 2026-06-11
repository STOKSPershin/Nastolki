package com.tbgames.app.core.network

import com.tbgames.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Dns
import java.net.InetAddress
import javax.inject.Singleton

private object IPv4OnlyDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return try {
            val addresses = Dns.SYSTEM.lookup(hostname)
            val ipv4 = addresses.filter { it is java.net.Inet4Address }
            ipv4.ifEmpty { addresses }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            httpEngine = OkHttp.create {
                config {
                    dns(IPv4OnlyDns)
                }
            }
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }
}

