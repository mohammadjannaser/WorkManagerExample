package com.balance.workmanagerexample

import android.app.Application
import androidx.work.Configuration

class MyApplication() : Application(), Configuration.Provider {
    override fun getWorkManagerConfiguration() =
            Configuration.Builder().setMinimumLoggingLevel(android.util.Log.DEBUG).build()
}