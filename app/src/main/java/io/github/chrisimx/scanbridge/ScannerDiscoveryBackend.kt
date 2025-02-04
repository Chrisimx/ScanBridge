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
import androidx.annotation.RequiresExtension
import androidx.compose.runtime.snapshots.SnapshotStateMap
import java.nio.charset.StandardCharsets
import java.util.concurrent.ForkJoinPool
import okhttp3.HttpUrl
import timber.log.Timber

private const val TAG = "ScannerDiscovery"

data class DiscoveredScanner(val name: String, val addresses: List<String>)

// Instantiate a new DiscoveryListener
class ScannerDiscovery(val nsdManager: NsdManager, val statefulScannerMap: SnapshotStateMap<String, DiscoveredScanner>) :
    NsdManager.DiscoveryListener {

    // Called as soon as service discovery begins.
    override fun onDiscoveryStarted(regType: String) {
        Timber.i("Service discovery started")
    }

    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 7)
    override fun onServiceFound(service: NsdServiceInfo) {
        // A service was found! Do something with it.
        Timber
            .d(
                "Service (${service.hashCode()}) discovery success ${service.hostAddresses} ${service.serviceType} ${service.serviceName} ${service.port} ${service.network}"
            )

        val serviceIdentifier = "${service.serviceName}.${service.serviceType}"
        if (statefulScannerMap.contains(serviceIdentifier)) {
            Timber.d("Ignored service. Got it already")
            return
        }

        val serviceInfoCallback = object : NsdManager.ServiceInfoCallback {

            override fun onServiceInfoCallbackRegistrationFailed(p0: Int) {
                Timber.tag(TAG).d("ServiceInfoCallBack (${this.hashCode()}) Registration failed!!! $p0")
            }

            override fun onServiceUpdated(p0: NsdServiceInfo) {
                Timber.tag(TAG).d("Service (${this.hashCode()}) updated! $p0")
                var rs = p0.attributes["rs"]?.toString(StandardCharsets.UTF_8) ?: "/"

                rs = if (rs.isEmpty()) "/" else "/$rs/"

                val urls = mutableListOf<String>()

                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    p0.hostAddresses
                } else {
                    listOf(p0.host)
                }

                for (address in addresses) {
                    val sanitizedURL = address.hostAddress!!.substringBefore('%')
                    val url = try {
                        HttpUrl.Builder()
                            .host(sanitizedURL)
                            .port(p0.port)
                            .encodedPath(rs)
                            .scheme("http")
                            .build()
                    } catch (e: Exception) {
                        Timber.tag(TAG).e("Couldn't built address from: ${address.hostAddress} Exception: $e")
                        continue
                    }

                    Timber.tag(TAG).d("Built URL: $url with address: ${address.hostAddress}")
                    urls.add(url.toString())
                }

                statefulScannerMap.put(serviceIdentifier, DiscoveredScanner(p0.serviceName, urls))
            }

            override fun onServiceLost() {
                statefulScannerMap.remove(serviceIdentifier)
                nsdManager.unregisterServiceInfoCallback(this)
                Timber.tag(TAG).d("Service was lost!")
            }

            override fun onServiceInfoCallbackUnregistered() {
                Timber.tag(TAG).d("ServiceInfoCallback (${this.hashCode()}) is getting unregistered!")
            }
        }

        if (service.serviceType != "_uscan._tcp.") {
            return
        }

        nsdManager.registerServiceInfoCallback(service, ForkJoinPool(1), serviceInfoCallback)
    }

    override fun onServiceLost(service: NsdServiceInfo) {
        // When the network service is no longer available.
        // Internal bookkeeping code goes here.
        Timber.i("service ${service.hashCode()} lost: $service")
    }

    override fun onDiscoveryStopped(serviceType: String) {
        Timber.i("Discovery stopped: $serviceType")
    }

    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
        Timber.e("Discovery failed: Error code:$errorCode")
        nsdManager.stopServiceDiscovery(this)
    }

    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
        Timber.e("Discovery failed: Error code:$errorCode")
        nsdManager.stopServiceDiscovery(this)
    }
}
