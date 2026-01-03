package io.github.chrisimx.scanbridge

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

fun Context.internetConnectivityFlow(): Flow<Boolean> = callbackFlow {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            trySend(true)
        }

        override fun onLost(network: Network) {
            trySend(false)
        }
    }

    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    cm.registerNetworkCallback(request, callback)

    // Initialize connection state
    trySend(isConnected(cm))

    awaitClose {
        cm.unregisterNetworkCallback(callback)
    }
}.distinctUntilChanged()

/**
 * Checks whether a validated internet connection is currently established
 */
private fun isConnected(cm: ConnectivityManager): Boolean {
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}


@Composable
fun rememberInternetConnectivityState(): State<Boolean> {
    val context = LocalContext.current.applicationContext

    return remember {
        context.internetConnectivityFlow()
    }.collectAsState(initial = false)
}
