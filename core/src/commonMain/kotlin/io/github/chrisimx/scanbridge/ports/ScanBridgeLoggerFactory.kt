package io.github.chrisimx.scanbridge.ports

import io.github.chrisimx.scanbridge.infrastructure.KmLogScanBridgeLogger
import io.github.chrisimx.scanbridge.ports.ScanBridgeLogger
import kotlin.reflect.KClass

interface ScanBridgeLoggerFactory {
    fun <T : Any> withClass(clazz: KClass<T>): ScanBridgeLogger
    fun withTag(tag: String): ScanBridgeLogger
}
