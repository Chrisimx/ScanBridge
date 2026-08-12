package io.github.chrisimx.scanbridge.ports

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name


interface ZipService {
    fun zip(files: List<PlatformFile>, output: PlatformFile, mapFileName: (PlatformFile) -> String = { it.name })
}
