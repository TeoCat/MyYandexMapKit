package com.example.myyandexmapkit

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class MainApplication : Application() {

    val MAPKIT_API_KEY = "9f096cd1-f146-47b5-bab6-c3def1b5c391"

    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey(MAPKIT_API_KEY)
    }
}