package io.github.chrisimx.scanbridge.model

import kotlinx.io.files.Path

data class ScanBridgeFile(val path: Path) {
    constructor(pathString: String) : this(Path(pathString))

    fun parent() = path.parent?.let {
        ScanBridgeFile(it)
    }

    fun child(fileName: String) = ScanBridgeFile(
        Path(path, fileName)
    )
}
