package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val alertDistance1: Int = 1000,
    val alertDistance2: Int = 500,
    val alertDistance3: Int = 200,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val voiceTtsEnabled: Boolean = true,
    val autoSyncEnabled: Boolean = true,
    val lastSyncTime: Long = 0L
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("radar_app_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            alertDistance1 = prefs.getInt(KEY_DIST_1, 1000),
            alertDistance2 = prefs.getInt(KEY_DIST_2, 500),
            alertDistance3 = prefs.getInt(KEY_DIST_3, 200),
            soundEnabled = prefs.getBoolean(KEY_SOUND, true),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true),
            voiceTtsEnabled = prefs.getBoolean(KEY_VOICE_TTS, true),
            autoSyncEnabled = prefs.getBoolean(KEY_AUTO_SYNC, true),
            lastSyncTime = prefs.getLong(KEY_LAST_SYNC, 0L)
        )
    }

    fun updateDistances(d1: Int, d2: Int, d3: Int) {
        prefs.edit()
            .putInt(KEY_DIST_1, d1)
            .putInt(KEY_DIST_2, d2)
            .putInt(KEY_DIST_3, d3)
            .apply()
        _settings.value = _settings.value.copy(
            alertDistance1 = d1,
            alertDistance2 = d2,
            alertDistance3 = d3
        )
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
        _settings.value = _settings.value.copy(soundEnabled = enabled)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
        _settings.value = _settings.value.copy(vibrationEnabled = enabled)
    }

    fun setVoiceTtsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_TTS, enabled).apply()
        _settings.value = _settings.value.copy(voiceTtsEnabled = enabled)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
        _settings.value = _settings.value.copy(autoSyncEnabled = enabled)
    }

    fun setLastSyncTime(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, timestamp).apply()
        _settings.value = _settings.value.copy(lastSyncTime = timestamp)
    }

    companion object {
        private const val KEY_DIST_1 = "alert_distance_1"
        private const val KEY_DIST_2 = "alert_distance_2"
        private const val KEY_DIST_3 = "alert_distance_3"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_VIBRATION = "vibration_enabled"
        private const val KEY_VOICE_TTS = "voice_tts_enabled"
        private const val KEY_AUTO_SYNC = "auto_sync_enabled"
        private const val KEY_LAST_SYNC = "last_sync_time"
    }
}
