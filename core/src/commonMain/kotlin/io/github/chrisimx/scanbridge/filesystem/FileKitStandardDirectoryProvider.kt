package io.github.chrisimx.scanbridge.filesystem

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.filesDir

class FileKitStandardDirectoryProvider : StandardDirectoryProvider {
    override val appFilesDirectory: PlatformFile
        get() = FileKit.filesDir
    override val appCacheDirectory: PlatformFile
        get() = FileKit.cacheDir
    override val databaseDirectory: PlatformFile
        get() = FileKit.databasesDir
}
