package io.github.chrisimx.scanbridge.db

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

interface ScanBridgeDbFactory {
    fun createInstance(): ScanBridgeDb
}

class DefaultScanBridgeDbFactory(val builderFactory: ScanBridgeDbBuilderFactory) : ScanBridgeDbFactory {
    override fun createInstance(): ScanBridgeDb {
        val dbBuilder = builderFactory.getBuilder()
        return dbBuilder
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}
