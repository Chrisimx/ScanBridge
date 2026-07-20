package io.github.chrisimx.scanbridge.model

import io.github.chrisimx.anyscan.LengthUnit
import kotlinx.serialization.Serializable

@Serializable
sealed class NumberValidationResult {
    data class Success(val value: LengthUnit) : NumberValidationResult()
    data class OutOfRange(val min: Double, val max: Double) : NumberValidationResult()
    data object NotANumber : NumberValidationResult()
}
