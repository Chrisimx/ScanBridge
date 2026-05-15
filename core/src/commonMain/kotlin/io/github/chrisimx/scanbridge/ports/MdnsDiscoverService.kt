package io.github.chrisimx.scanbridge.ports

import io.github.chrisimx.scanbridge.model.MdnsService
import kotlinx.coroutines.flow.StateFlow

interface MdnsDiscoverService : AutoCloseable {
    val foundServices: StateFlow<Map<String, MdnsService>>
    val serviceType: String?

    fun start(serviceType: String)
    fun stop()

    override fun close()
}
