package io.github.chrisimx.scanbridge.filesystem

import io.github.vinceglb.filekit.PlatformFile

interface StandardDirectoryProvider {
    val appFilesDirectory: PlatformFile
    val appCacheDirectory: PlatformFile
    val databaseDirectory: PlatformFile
}
