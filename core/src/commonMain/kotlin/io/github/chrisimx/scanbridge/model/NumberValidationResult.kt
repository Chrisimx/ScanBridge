package io.github.chrisimx.scanbridge.model

import kotlinx.serialization.Serializable

@Serializable
sealed class NumberValidationResult {
    data class Success(val value: Double) : NumberValidationResult()
    data class OutOfRange(val min: Double, val max: Double) : NumberValidationResult()
    data object NotANumber : NumberValidationResult()
}
