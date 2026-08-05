package com.rstrategy.app

import android.app.Application
import android.content.Context

class RStrategyApp : Application() {

    companion object {
        lateinit var instance: RStrategyApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
