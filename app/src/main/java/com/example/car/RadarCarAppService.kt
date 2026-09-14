package com.example.car

import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import android.content.Intent

class RadarCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return RadarCarSession()
    }
}

class RadarCarSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        return RadarCarScreen(carContext)
    }
}
