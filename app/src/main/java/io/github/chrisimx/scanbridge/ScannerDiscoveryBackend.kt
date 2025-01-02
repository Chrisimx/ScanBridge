/*
 *     Copyright (C) 2024-2025 Christian Nagel and contributors
 *
 *     This file is part of ScanBridge.
 *
 *     ScanBridge is free software: you can redistribute it and/or modify it under the terms of
 *     the GNU General Public License as published by the Free Software Foundation, either
 *     version 3 of the License, or (at your option) any later version.
 *
 *     ScanBridge is distributed in the hope that it will be useful, but WITHOUT ANY
 *     WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *     FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along with eSCLKt.
 *     If not, see <https://www.gnu.org/licenses/>.
 *
 *     SPDX-License-Identifier: GPL-3.0-or-later
 */

package io.github.chrisimx.scanbridge

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresExtension
import androidx.compose.runtime.snapshots.SnapshotStateMap
import okhttp3.HttpUrl
import java.nio.charset.StandardCharsets
import java.util.concurrent.ForkJoinPool

data class DiscoveredScanner(val name: String, val addresses: List<String>)

// Instantiate a new DiscoveryListener
class ScannerDiscovery(
    val nsdManager: NsdManager,
    val statefulScannerMap: SnapshotStateMap<String, DiscoveredScanner>
) : NsdManager.DiscoveryListener {

    val TAG = "ScannerDiscovery"
    // Called as soon as service discovery begins.
    override fun onDiscoveryStarted(regType: String) {
        Log.d(TAG, "Service discovery started")
    }

    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 7)
    override fun onServiceFound(service: NsdServiceInfo) {
        // A service was found! Do something with it.
        Log.d(TAG, "Service (${service.hashCode()}) discovery success ${service.hostAddresses} ${service.serviceType} ${service.serviceName} ${service.port} ${service.network}")

        val serviceIdentifier = "${service.serviceName}.${service.serviceType}"
        if (statefulScannerMap.contains(serviceIdentifier)) {
            Log.d(TAG, "Ignored service. Got it already")
            return
        }

        val serviceInfoCallback = object : NsdManager.ServiceInfoCallback {

            override fun onServiceInfoCallbackRegistrationFailed(p0: Int) {
                Log.d(TAG, "ServiceInfoCallBack (${this.hashCode()}) Registration failed!!! $p0")
            }

            override fun onServiceUpdated(p0: NsdServiceInfo) {
                Log.d(TAG, "Service (${this.hashCode()}) updated! $p0")
                val rs = p0.attributes.get("rs")?.toString(StandardCharsets.UTF_8) ?: ""

                val urls = mutableListOf<String>()

                for (address in p0.hostAddresses) {
                    val url = HttpUrl.Builder()
                        .host(address.hostAddress!!)
                        .addPathSegment(rs)
                        .scheme("http")
                        .build()
                    Log.d(TAG, "Built URL: $url")
                    urls.add(url.toString())
                }

                statefulScannerMap.put(serviceIdentifier, DiscoveredScanner(p0.serviceName, urls))
            }

            override fun onServiceLost() {
                statefulScannerMap.remove(serviceIdentifier)
                nsdManager.unregisterServiceInfoCallback(this)
                Log.d(TAG, "Service was lost!")
            }

            override fun onServiceInfoCallbackUnregistered() {
                Log.d(TAG, "ServiceInfoCallback (${this.hashCode()}) is getting unregistered!")
            }

        }

        if (service.serviceType != "_uscan._tcp.") {
            return
        }

        nsdManager.registerServiceInfoCallback(service, ForkJoinPool(1), serviceInfoCallback )
    }

    override fun onServiceLost(service: NsdServiceInfo) {
        // When the network service is no longer available.
        // Internal bookkeeping code goes here.
        Log.e(TAG, "service ${service.hashCode()} lost: $service")
    }

    override fun onDiscoveryStopped(serviceType: String) {
        Log.i(TAG, "Discovery stopped: $serviceType")
    }

    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
        Log.e(TAG, "Discovery failed: Error code:$errorCode")
        nsdManager.stopServiceDiscovery(this)
    }

    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
        Log.e(TAG, "Discovery failed: Error code:$errorCode")
        nsdManager.stopServiceDiscovery(this)
    }
}