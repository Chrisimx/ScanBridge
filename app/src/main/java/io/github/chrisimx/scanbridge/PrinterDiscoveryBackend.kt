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
import android.os.ext.SdkExtensions
import androidx.compose.runtime.snapshots.SnapshotStateMap
import java.nio.charset.StandardCharsets
import java.util.concurrent.ForkJoinPool
import okhttp3.HttpUrl
import timber.log.Timber

private const val TAG = "PrinterDiscovery"

data class DiscoveredPrinter(val name: String, val addresses: List<String>)

class PrinterDiscovery(
    val nsdManager: NsdManager,
    val isSecure: Boolean,
    val statefulPrinterMap: SnapshotStateMap<String, DiscoveredPrinter>
) : NsdManager.DiscoveryListener {

    override fun onDiscoveryStarted(regType: String) {
        Timber.i("Printer discovery started")
    }

    override fun onServiceFound(service: NsdServiceInfo) {
        Timber
            .d(
                "Service (${service.hashCode()}) discovery success ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) service.hostAddresses else service.host} ${service.serviceType} ${service.serviceName} ${service.port}"
            )

        val serviceIdentifier = "${service.serviceName}.${service.serviceType}"
        if (statefulPrinterMap.contains(serviceIdentifier)) {
            Timber.d("Ignored service. Got it already")
            return
        }
        if (!isSecure && service.serviceType != "_ipp._tcp.") {
            return
        }
        if (isSecure && service.serviceType != "_ipps._tcp.") {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= 7) {
            val serviceInfoCallback =
                object : NsdManager.ServiceInfoCallback {

                    override fun onServiceInfoCallbackRegistrationFailed(p0: Int) {
                        Timber.tag(TAG).d("ServiceInfoCallBack (${this.hashCode()}) Registration failed!!! $p0")
                    }

                    override fun onServiceUpdated(p0: NsdServiceInfo) {
                        Timber.tag(TAG).d("Service (${this.hashCode()}) updated! $p0")
                        var rp = p0.attributes["rp"]?.toString(StandardCharsets.UTF_8) ?: "/ipp/print"

                        rp = if (rp.isEmpty()) "/ipp/print" else "/$rp/"

                        val urls = mutableListOf<String>()

                        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            p0.hostAddresses
                        } else {
                            listOf(p0.host)
                        }

                        for (address in addresses) {
                            if (address.isLinkLocalAddress) {
                                Timber.tag(TAG).d("Ignoring link local address: ${address.hostAddress}")
                                continue
                            }
                            val sanitizedURL = address.hostAddress!!.substringBefore('%')
                            val url = try {
                                HttpUrl.Builder()
                                    .host(sanitizedURL)
                                    .port(p0.port)
                                    .scheme(if (isSecure) "ipps" else "ipp")
                                    .addPathSegment(rp.trim('/'))
                                    .build()
                                    .toString()
                            } catch (e: Exception) {
                                Timber.tag(TAG).d("Failed to build URL: $e")
                                continue
                            }

                            urls.add(url)
                        }

                        if (urls.isNotEmpty()) {
                            statefulPrinterMap[serviceIdentifier] = DiscoveredPrinter(p0.serviceName, urls)
                        }
                    }

                    override fun onServiceLost() {
                        Timber.tag(TAG).d("Service (${this.hashCode()}) lost!")
                        statefulPrinterMap.remove(serviceIdentifier)
                    }

                    override fun onServiceInfoCallbackUnregistered() {
                        Timber.tag(TAG).d("ServiceInfoCallBack (${this.hashCode()}) unregistered!")
                    }
                }

            Timber.tag(TAG).d("Registering ServiceInfoCallback (${serviceInfoCallback.hashCode()}) for $serviceIdentifier")
            nsdManager.registerServiceInfoCallback(service, ForkJoinPool.commonPool(), serviceInfoCallback)
        } else {
            nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Timber.e("Resolve failed: $errorCode")
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    Timber.d("Resolve Succeeded. $serviceInfo")

                    val sanitizedURL = serviceInfo.host.hostAddress!!.substringBefore('%')
                    val url = try {
                        HttpUrl.Builder()
                            .host(sanitizedURL)
                            .port(serviceInfo.port)
                            .scheme(if (isSecure) "ipps" else "ipp")
                            .addPathSegment("ipp")
                            .addPathSegment("print")
                            .build()
                            .toString()
                    } catch (e: Exception) {
                        Timber.tag(TAG).d("Failed to build URL: $e")
                        return
                    }

                    statefulPrinterMap[serviceIdentifier] = DiscoveredPrinter(serviceInfo.serviceName, listOf(url))
                }
            })
        }
    }

    override fun onServiceLost(service: NsdServiceInfo) {
        Timber.e("service lost: $service")
        val serviceIdentifier = "${service.serviceName}.${service.serviceType}"
        statefulPrinterMap.remove(serviceIdentifier)
    }

    override fun onDiscoveryStopped(serviceType: String) {
        Timber.i("Discovery stopped: $serviceType")
    }

    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
        Timber.e("Discovery failed: Error code: $errorCode")
        nsdManager.stopServiceDiscovery(this)
    }

    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
        Timber.e("Discovery failed: Error code: $errorCode")
        nsdManager.stopServiceDiscovery(this)
    }
}