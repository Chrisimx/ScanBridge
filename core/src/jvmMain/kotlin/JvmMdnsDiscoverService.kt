import io.github.chrisimx.scanbridge.logging.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.model.MdnsService
import io.github.chrisimx.scanbridge.ports.MdnsDiscoverService
import java.util.concurrent.atomic.AtomicBoolean
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class JvmMdnsDiscoverService(
    loggerFactory: ScanBridgeLoggerFactory
) : MdnsDiscoverService {
    val logger = loggerFactory.withClass(this::class)

    var _serviceType: String? = null
    override val serviceType: String?
        get() = _serviceType

    val _foundServices = MutableStateFlow(emptyMap<String, MdnsService>())

    override val foundServices: StateFlow<Map<String, MdnsService>> = _foundServices.asStateFlow()
    private val started = AtomicBoolean(false)
    val jmdns = JmDNS.create()

    private fun ServiceInfo.getTxtRecords(): Map<String, ByteArray> {
        val propertyNames = this.propertyNames
        return propertyNames
            .asSequence()
            .associateWith {
                this.getPropertyBytes(it)
            }
    }

    override fun start(serviceType: String) {
        if (!started.compareAndSet(false, true)) {
            logger.debug { "JvmMdnsDiscoverService already started" }
            return
        }
        logger.debug { "Starting JvmMdnsDiscoverService with $serviceType" }

        this._serviceType = serviceType

        jmdns.addServiceListener(serviceType, object : ServiceListener {
            override fun serviceAdded(event: ServiceEvent) {
                logger.debug { "Service added: ${event.name}" }
            }

            override fun serviceRemoved(event: ServiceEvent) {
                logger.debug { "Service removed: ${event.name}" }
                _foundServices.update {
                    it.filterKeys { key -> key != event.name }
                }
            }

            override fun serviceResolved(event: ServiceEvent) {
                logger.debug { "Service resolved: ${event.name}" }
                val mdnsService: MdnsService = MdnsService(
                    event.info.name,
                    event.info.type,
                    event.info.port,
                    event.info.inetAddresses.map {
                        it.toMultiplatformIpAddress()
                    },
                    event.info.getTxtRecords()
                )

                _foundServices.update {
                    it + (event.name to mdnsService)
                }
            }
        })
    }

    override fun stop() {
        if (!started.compareAndSet(true, false)) {
            return
        }

        logger.debug { "Stopping JvmMdnsDiscoverService" }

        jmdns.unregisterAllServices()
        _serviceType = null
    }

    override fun close() {
        logger.debug { "Closing JvmMdnsDiscoverService" }
        jmdns.close()
    }
}
