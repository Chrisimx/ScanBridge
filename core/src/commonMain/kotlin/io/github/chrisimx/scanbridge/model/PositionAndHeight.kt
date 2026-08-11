package io.github.chrisimx.scanbridge.model

data class PositionAndHeight<T: Number>(
    val x: T,
    val y: T,
    val height: T
)
