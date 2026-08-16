package io.github.chrisimx.scanbridge.export.zip

import io.github.chrisimx.scanbridge.model.FileToBeExported
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name


interface ZipService {
    fun zipFlat(files: List<FileToBeExported>, output: PlatformFile, mapFileName: (PlatformFile) -> String = { it.name })
}
