package com.rememberforever

import android.app.Application
import com.rememberforever.core.data.AppContainer

class RememberForeverApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
