package io.github.chrisimx.scanbridge.logging

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
