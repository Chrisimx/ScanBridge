package io.github.chrisimx.scanbridge

import AndroidMdnsDiscoverService
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisimx.scanbridge.infrastructure.KmLogScanBridgeLoggerFactory
import java.net.ServerSocket
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidMdnsDiscoverServiceInstrumentedTest {

    private val context: Context =
        ApplicationProvider.getApplicationContext()

    private val nsdManager: NsdManager =
        context.getSystemService(NsdManager::class.java)

    private var registeredServiceName: String? = null

    @Test
    fun discoversRegisteredMdnsService() = runBlocking {
        val serviceType = "_scanbridge-test._tcp"
        val serviceName = "scanbridge-test-${System.currentTimeMillis()}"

        val port = ServerSocket(0).use { it.localPort }

        val registration = registerTestService(
            serviceName = serviceName,
            serviceType = serviceType,
            port = port,
            attributes = mapOf(
                "test" to "true".encodeToByteArray()
            )
        )

        val discoverService = AndroidMdnsDiscoverService(
            appContext = context,
            loggerFactory = KmLogScanBridgeLoggerFactory()
        )

        try {
            discoverService.start(serviceType)

            val services = withTimeout(15_000) {
                discoverService.foundServices.first { services ->
                    services.values.any {
                        it.serviceType == serviceType &&
                            it.port == port &&
                            it.serviceName.contains(serviceName)
                    }
                }
            }

            val discovered = services.values.first {
                it.serviceType == serviceType &&
                    it.port == port &&
                    it.serviceName.contains(serviceName)
            }

            assertEquals(serviceType, discovered.serviceType)
            assertEquals(port, discovered.port)
            assertTrue(discovered.addresses.isNotEmpty())
            assertTrue(discovered.txtAttributes.containsKey("test"))
        } finally {
            discoverService.close()
            unregisterTestService(registration)
        }
    }

    @Test
    fun stopStopsDiscoveryWithoutCrashing() = runBlocking {
        val discoverService = AndroidMdnsDiscoverService(
            appContext = context,
            loggerFactory = KmLogScanBridgeLoggerFactory()
        )

        discoverService.start("_scanbridge-test._tcp.")
        discoverService.stop()
        discoverService.stop()

        assertTrue(discoverService.foundServices.value.isEmpty())
    }

    private suspend fun registerTestService(
        serviceName: String,
        serviceType: String,
        port: Int,
        attributes: Map<String, ByteArray> = emptyMap()
    ): NsdManager.RegistrationListener {
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = serviceType
            this.port = port

            attributes.forEach { (key, value) ->
                setAttribute(key, value.toString(Charsets.UTF_8))
            }
        }

        return suspendCancellableCoroutine { continuation ->
            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                    registeredServiceName = serviceInfo.serviceName
                    continuation.resume(this)
                }

                override fun onRegistrationFailed(
                    serviceInfo: NsdServiceInfo,
                    errorCode: Int
                ) {
                    continuation.resumeWithException(
                        AssertionError("NSD registration failed: $errorCode")
                    )
                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit

                override fun onUnregistrationFailed(
                    serviceInfo: NsdServiceInfo,
                    errorCode: Int
                ) = Unit
            }

            nsdManager.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                listener
            )

            continuation.invokeOnCancellation {
                runCatching {
                    nsdManager.unregisterService(listener)
                }
            }
        }
    }

    private fun unregisterTestService(
        listener: NsdManager.RegistrationListener
    ) {
        runCatching {
            nsdManager.unregisterService(listener)
        }
    }
}
