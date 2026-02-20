package io.github.chrisimx.scanbridge.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

fun <T, R> StateFlow<T>.derived(
    scope: CoroutineScope,
    mapper: (T) -> R
): StateFlow<R> = map(mapper)
    .stateIn(
        scope = scope,
        started = SharingStarted.Lazily,
        initialValue = mapper(value)
    )
