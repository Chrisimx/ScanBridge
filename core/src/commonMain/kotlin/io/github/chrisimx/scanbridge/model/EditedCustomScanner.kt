package io.github.chrisimx.scanbridge.model

import io.github.chrisimx.scanbridge.db.entities.CustomScanner

/**
 * A custom scanner that is either new or being edited.
 *
 * Used for describing whether we are currently editing a custom scanner or creating a new one.
 */
sealed class EditedCustomScanner {
    data object New : EditedCustomScanner()
    data class EditingOld(val scanner: CustomScanner) : EditedCustomScanner()
}
