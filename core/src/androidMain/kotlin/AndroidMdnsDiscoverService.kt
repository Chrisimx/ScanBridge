import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import io.github.chrisimx.scanbridge.model.IpAddress
import io.github.chrisimx.scanbridge.model.MdnsService
import io.github.chrisimx.scanbridge.ports.MdnsDiscoverService
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AndroidMdnsDiscoverService(
    val appContext: Context,
    val loggerFactory: ScanBridgeLoggerFactory
) : MdnsDiscoverService {
    private val logger = loggerFactory.withClass(this::class)

    private val _registeredListeners = mutableListOf<NsdManager.DiscoveryListener>()
    private val _serviceInfoCallbacks =
        mutableListOf<NsdManager.ServiceInfoCallback>()

    override val foundServices: StateFlow<Map<String, MdnsService>>
        get() = _foundServices.asStateFlow()

    private val _foundServices = MutableStateFlow(mapOf<String, MdnsService>())

    private val nsdManager = appContext.getSystemService(NsdManager::class.java)

    private val started = AtomicBoolean(false)

    override val serviceType: String?
        get() = _serviceType

    var _serviceType: String? = null

    //private val callbackExecutor = Executors.newSingleThreadExecutor()

    override fun start(serviceType: String) {
        if (started.getAndSet(true)) {
            logger.warn { "Already started" }
            return
        }

        _serviceType = serviceType

        if (nsdManager == null) {
            started.set(false)
            logger.error { "Couldn't get NsdManager service" }
            return
        }

        nsdManager.discoverServices(
            serviceType,
            NsdManager.PROTOCOL_DNS_SD,
            createDiscoveryListener()
        )
    }

    override fun stop() {
        if (!started.getAndSet(false)) {
            logger.warn { "Already stopped" }
            return
        }

        _serviceType = null

        for (listener in _registeredListeners) {
            nsdManager.stopServiceDiscovery(listener)
        }

        for (callback in _serviceInfoCallbacks) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= 7) {
                nsdManager.unregisterServiceInfoCallback(callback)
            }
        }

        _serviceInfoCallbacks.clear()
        _registeredListeners.clear()
    }

    private fun createDiscoveryListener(): NsdManager.DiscoveryListener {
        val discoveryListener =  object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                logger.info { "Service discovery started: $serviceType" }
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                logger.info { "Service discovery stopped: $serviceType" }
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo == null) {
                    logger.warn { "onServiceFound: Service info was null" }
                    return
                }

                logger.info { "Service with name ${serviceInfo.serviceName} and type ${serviceInfo.serviceType} found" }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= 7) {
                    val serviceInfoCallback = createServiceInfoCallback(serviceInfo)
                    nsdManager.registerServiceInfoCallback(serviceInfo, ForkJoinPool(1), serviceInfoCallback)
                } else {
                    logger.debug { "ServiceInfoCallback not supported. Falling back to ResolveListener" }
                    nsdManager.resolveService(
                        serviceInfo,
                        createResolveListener()
                    )
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                logger.info { "Service with name ${serviceInfo?.serviceName} and type ${serviceInfo?.serviceType} lost" }

                val info = serviceInfo ?: return

                _foundServices.update {
                    it.filterKeys { key ->
                        key != getServiceUniqueIdentifier(info)
                    }
                }
            }

            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                logger.error { "Service discovery failed: Error code: $errorCode" }
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                logger.error { "Service discovery failed: Error code: $errorCode" }
                nsdManager.stopServiceDiscovery(this)
            }
        }
        _registeredListeners.add(discoveryListener)
        return discoveryListener
    }

    private fun createResolveListener(): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                logger.error { "Resolve failed for ${serviceInfo?.serviceName} and ${serviceInfo?.serviceType}: $errorCode" }
            }

            override fun onServiceResolved(resolvedService: NsdServiceInfo?) {
                if (resolvedService == null) {
                    logger.warn { "onServiceResolved: Service info was null" }
                    return
                }

                logger.debug { "Resolved service: ${resolvedService.serviceName} and ${resolvedService.serviceType}" }

                val mdnsService = nsdServiceInfoToMdnsService(resolvedService)

                val serviceIdentifier = getServiceUniqueIdentifier(resolvedService)

                updateServiceStore(serviceIdentifier, mdnsService)
            }
        }
    }

    private fun nsdServiceInfoToMdnsService(serviceInfo: NsdServiceInfo): MdnsService {

        return MdnsService(
            serviceInfo.serviceName,
            serviceInfo.serviceType,
            serviceInfo.port,
            getAddressesOfNsdService(serviceInfo),
            serviceInfo.attributes
        )
    }

    private fun getAddressesOfNsdService(serviceInfo: NsdServiceInfo): List<IpAddress> {
        val inetAddresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            serviceInfo.hostAddresses
        } else {
            listOf(serviceInfo.host)
        }

        return inetAddresses.map { it.toMultiplatformIpAddress() }
    }

    private fun getServiceUniqueIdentifier(serviceInfo: NsdServiceInfo): String {
        return "${serviceInfo.serviceName}.${serviceInfo.serviceType}"
    }

    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 7)
    private fun createServiceInfoCallback(originalServiceInfo: NsdServiceInfo): NsdManager.ServiceInfoCallback {
        val serviceInfoCallback =  object : NsdManager.ServiceInfoCallback {
            override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                logger.error { "ServiceInfoCallback (${this.hashCode()}) registration failed: $errorCode" }
            }

            override fun onServiceInfoCallbackUnregistered() {
                logger.debug { "ServiceInfoCallback (${this.hashCode()}) is being unregistered" }
            }

            override fun onServiceLost() {
                val serviceIdentifier = getServiceUniqueIdentifier(originalServiceInfo)

                _serviceInfoCallbacks.remove(this)
                nsdManager.unregisterServiceInfoCallback(this)
                _foundServices.update {
                    val updateMap = it.toMutableMap()
                    updateMap.remove(serviceIdentifier)
                    updateMap
                }

                logger.debug { "Service with identifier $serviceIdentifier was lost" }
            }

            override fun onServiceUpdated(updatedServiceInfo: NsdServiceInfo) {
                val serviceIdentifier = getServiceUniqueIdentifier(updatedServiceInfo)
                logger.debug { "Service with identifier $serviceIdentifier was updated" }

                val mdnsService = nsdServiceInfoToMdnsService(updatedServiceInfo)

                updateServiceStore(serviceIdentifier, mdnsService)
            }
        }
        _serviceInfoCallbacks.add(serviceInfoCallback)
        return serviceInfoCallback
    }

    private fun updateServiceStore(
        serviceIdentifier: String,
        mdnsService: MdnsService
    ) {
        _foundServices.update {
            it + (serviceIdentifier to mdnsService)
        }
    }

    override fun close() {
        stop()
        //callbackExecutor.shutdown()
    }
}
