package com.tbgames.app.core.common

object Constants {
    const val MIN_NICKNAME_LENGTH = 3
    const val MAX_NICKNAME_LENGTH = 16
    val NICKNAME_REGEX = Regex("^[a-zA-Zа-яА-ЯёЁ0-9_]+$")

    const val PRESET_AVATARS_COUNT = 20
    const val AVATAR_TYPE_PRESET = "preset"
    const val AVATAR_TYPE_CUSTOM = "custom"

    const val LOBBY_CHANNEL = "lobby"
    const val ROOM_CHANNEL_PREFIX = "room:"

    const val AVATARS_BUCKET = "avatars"
    const val MAX_AVATAR_SIZE_BYTES = 5 * 1024 * 1024 // 5MB

    const val DEFAULT_MAX_PLAYERS = 4
    const val MIN_PLAYERS_IN_ROOM = 2
    const val MAX_PLAYERS_IN_ROOM = 8

    object RoomStatus {
        const val WAITING = "waiting"
        const val PLAYING = "playing"
        const val FINISHED = "finished"
    }

    object PlayerStatus {
        const val IN_LOBBY = "in_lobby"
        const val IN_ROOM = "in_room"
        const val IN_GAME = "in_game"
    }

    object PrefsKeys {
        const val SOUND_ENABLED = "sound_enabled"
        const val VIBRATION_ENABLED = "vibration_enabled"
        const val THEME_MODE = "theme_mode"
    }

    object ThemeMode {
        const val LIGHT = "light"
        const val DARK = "dark"
        const val SYSTEM = "system"
    }
}
