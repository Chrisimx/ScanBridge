package io.github.chrisimx.scanbridge.data.model

import io.github.chrisimx.scanbridge.db.entities.CustomScanner

sealed class EditedCustomScanner {
    data object New : EditedCustomScanner()
    data class EditingOld(val scanner: CustomScanner) : EditedCustomScanner()
}
