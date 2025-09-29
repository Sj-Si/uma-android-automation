package com.steve1316.uma_android_automation

import android.app.Application
import android.content.Context

class MainApplication: Application() {
    companion object {
        private lateinit var instance: MainApplication
        fun getApplicationContext(): Context {
            return instance.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
