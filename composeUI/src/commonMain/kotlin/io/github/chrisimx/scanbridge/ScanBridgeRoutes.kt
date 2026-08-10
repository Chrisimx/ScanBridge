package io.github.chrisimx.scanbridge

import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.Serializable


@Serializable
sealed interface BaseRoute

@Serializable
@SerialName("StartUpScreenRoute")
object StartUpScreenRoute : BaseRoute

@Serializable
@SerialName("ScannerRoute")
@OptIn(ExperimentalSerializationApi::class)
data class ScannerRoute(
    val scannerName: String,
    @JsonNames("scannerHandle", "scannerURL")
    val scannerHandleString: String,
    val protocolId: String = "eSCL",
    val sessionID: String
) : BaseRoute

@Serializable
@SerialName("CropImageRoute")
data class CropImageRoute(val scanId: String, val returnRoute: String) : BaseRoute

@Serializable
@SerialName("ErrorRoute")
data class ErrorRoute(val error: String) : BaseRoute

fun NavBackStackEntry.toTypedRoute(): BaseRoute? {
    return when (destination.route) {
        "StartUpScreenRoute" -> this.toRoute<StartUpScreenRoute>()

        "CropImageRoute/{scanId}/{pageIdx}/{returnRoute}" -> {
            this.toRoute<CropImageRoute>()
        }

        "ScannerRoute/{scannerName}/{scannerHandleString}/{sessionID}?protocolId={protocolId}" -> {
            this.toRoute<ScannerRoute>()
        }

        "ErrorRoute/{error}" -> {
            this.toRoute<ErrorRoute>()
        }

        else -> null
    }
}
