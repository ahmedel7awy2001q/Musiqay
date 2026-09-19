package com.musiqay.app

import android.app.Application
import com.musiqay.app.data.MediaStoreRepository
import com.musiqay.app.data.MusiqayDatabase
import com.musiqay.app.data.SettingsRepository
import com.musiqay.app.playback.PlayerController

class MusiqayApplication : Application() {
    val database by lazy { MusiqayDatabase.get(this) }
    val mediaStoreRepository by lazy { MediaStoreRepository(this) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val playerController by lazy { PlayerController(this) }

    override fun onCreate() {
        super.onCreate()
        playerController.connect()
    }
}
