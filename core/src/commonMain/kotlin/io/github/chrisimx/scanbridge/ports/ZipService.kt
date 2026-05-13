package io.github.chrisimx.scanbridge.ports

import io.github.chrisimx.scanbridge.model.ScanBridgeFile

interface ZipService {
    fun zip(files: List<ScanBridgeFile>, output: ScanBridgeFile, mapFileName: (ScanBridgeFile) -> String = { it.path.name })
}
