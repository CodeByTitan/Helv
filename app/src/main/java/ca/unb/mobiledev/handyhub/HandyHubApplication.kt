package ca.unb.mobiledev.handyhub

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HandyHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
