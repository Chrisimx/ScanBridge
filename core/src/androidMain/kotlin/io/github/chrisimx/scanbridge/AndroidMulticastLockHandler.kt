package io.github.chrisimx.scanbridge

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.ports.multicast.MulticastLockHandler

class AndroidMulticastLockHandler(val application: Application, private val loggerFactory: ScanBridgeLoggerFactory) : MulticastLockHandler {

    private val logger = loggerFactory.withClass(this::class)
    private val wifiManager =
        application.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val lock =
        wifiManager.createMulticastLock("wsd_discovery")

    override fun acquire() {
        if (!lock.isHeld) {
            logger.debug { "Acquiring multicast lock" }
            lock.acquire()
        }
    }

    override fun release() {
        if (lock.isHeld) {
            logger.debug { "Releasing multicast lock" }
            lock.release()
        }
    }
}
