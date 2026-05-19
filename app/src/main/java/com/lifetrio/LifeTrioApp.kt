package com.lifetrio

import android.app.Application
import com.lifetrio.core.data.AppContainer

class LifeTrioApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
