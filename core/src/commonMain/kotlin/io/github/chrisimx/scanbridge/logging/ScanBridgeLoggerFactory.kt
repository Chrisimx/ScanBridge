package io.github.chrisimx.scanbridge.logging

import kotlin.reflect.KClass

interface ScanBridgeLoggerFactory {
    fun <T : Any> withClass(clazz: KClass<T>): ScanBridgeLogger
    fun withTag(tag: String): ScanBridgeLogger
}
