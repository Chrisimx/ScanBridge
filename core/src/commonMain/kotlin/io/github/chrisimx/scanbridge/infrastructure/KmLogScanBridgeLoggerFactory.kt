package io.github.chrisimx.scanbridge.infrastructure

import io.github.chrisimx.scanbridge.infrastructure.KmLogScanBridgeLogger
import io.github.chrisimx.scanbridge.ports.ScanBridgeLogger
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import kotlin.reflect.KClass

class KmLogScanBridgeLoggerFactory : ScanBridgeLoggerFactory {
    override fun <T : Any> withClass(clazz: KClass<T>): ScanBridgeLogger {
        val tag = clazz.simpleName ?: "Unknown"
        return KmLogScanBridgeLogger(
            tag
        )
    }

    override fun withTag(tag: String): ScanBridgeLogger = KmLogScanBridgeLogger(tag)
}
