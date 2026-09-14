package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.preference.PreferenceManager
import com.example.data.local.RadarDatabase
import com.example.data.repository.RadarRepository
import com.example.data.repository.SettingsRepository
import org.osmdroid.config.Configuration

class RadarApplication : Application() {

    lateinit var database: RadarDatabase
        private set

    lateinit var radarRepository: RadarRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var navigationRepository: com.example.data.repository.NavigationRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName

        // Initialize Local Storage & Repositories
        database = RadarDatabase.getInstance(this)
        settingsRepository = SettingsRepository(this)
        radarRepository = RadarRepository(database.radarDao(), settingsRepository)
        navigationRepository = com.example.data.repository.NavigationRepository(database.radarDao())

        // Create Notification Channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Foreground Tracking Service Channel (Silent/Low)
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "Radar Takip Servisi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Sürüş modu sırasında arka plan radar taraması bildirimi"
                setShowBadge(false)
            }

            // 2. High-Priority Alert Channel (Sound + Vibration + Heads-up)
            val alertSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "Radar Yaklaşma Uyarıları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Hız radarlarına yaklaşırken verilen kritik sesli ve görsel uyarılar"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 150, 350)
                setSound(alertSoundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        const val CHANNEL_SERVICE_ID = "radar_tracking_service_channel"
        const val CHANNEL_ALERT_ID = "radar_proximity_alert_channel"

        lateinit var instance: RadarApplication
            private set
    }
}
