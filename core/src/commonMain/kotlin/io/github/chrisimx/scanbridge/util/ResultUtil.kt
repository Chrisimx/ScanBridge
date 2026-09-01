package io.github.chrisimx.scanbridge.util

import kotlinx.coroutines.CancellationException

fun <T> Result<T>.getOrNullIgnoreCancellation(): T? {
    onFailure {
        if (it is CancellationException) {
            throw it
        }
    }
    return getOrNull()
}
