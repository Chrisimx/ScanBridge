package io.github.chrisimx.scanbridge.model

import io.github.vinceglb.filekit.PlatformFile

data class FileToBeExported(
    val file: PlatformFile,
    val exportedFileName: String
)
