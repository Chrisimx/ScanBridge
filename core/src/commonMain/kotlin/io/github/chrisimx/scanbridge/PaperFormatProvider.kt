package io.github.chrisimx.scanbridge

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface PaperFormatProvider {
    val formats: StateFlow<List<PaperFormat>>
}

class DefaultPaperFormatProvider : PaperFormatProvider {
    override val formats: StateFlow<List<PaperFormat>> = MutableStateFlow(loadDefaultFormats()).asStateFlow()
}
