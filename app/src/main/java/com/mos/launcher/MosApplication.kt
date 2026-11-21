package com.mos.launcher

import android.app.Application
import com.mos.launcher.di.AppContainer

class MosApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
