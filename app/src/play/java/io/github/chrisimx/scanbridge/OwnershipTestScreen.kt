package io.github.chrisimx.scanbridge

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import timber.log.Timber

@Composable
fun OwnershipTestScreen(innerPadding: PaddingValues) {
    val context = LocalContext.current

    LaunchedEffect (Unit) {
        val ops = OwnershipProofService(context.applicationContext)
        val proof = ops.requestOwnershipProof(20);
        if (proof is LicensingRequestResult.OwnershipProof) {
            Timber.d("Proof: ${proof.responseData.originalResponse} Sig: ${proof.signature}")
        }

    }
}
