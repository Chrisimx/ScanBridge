package io.github.chrisimx.scanbridge

import android.app.Application
import timber.log.Timber

class ScanBridgeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
