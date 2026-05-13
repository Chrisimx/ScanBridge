package io.github.chrisimx.scanbridge.db

import androidx.room.RoomDatabase

interface ScanBridgeDbBuilderFactory {
    fun getBuilder(): RoomDatabase.Builder<ScanBridgeDb>
}
