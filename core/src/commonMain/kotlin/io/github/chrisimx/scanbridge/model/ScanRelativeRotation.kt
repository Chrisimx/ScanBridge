package io.github.chrisimx.scanbridge.model

enum class ScanRelativeRotation {
    Rotated,
    Original
}

fun ScanRelativeRotation.toggleRotation() = when (this) {
    ScanRelativeRotation.Rotated -> ScanRelativeRotation.Original
    ScanRelativeRotation.Original -> ScanRelativeRotation.Rotated
}
