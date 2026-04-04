package io.github.chrisimx.scanbridge

import com.diamondedge.logging.KmLog
import com.diamondedge.logging.logging
import org.koin.core.annotation.InjectedParam
import org.koin.mp.KoinPlatform.getKoin

class KmLogScanBridgeLogger(
    @InjectedParam
    tag: String
) : ScanBridgeLogger {
    private val logger: KmLog = logging(tag)

    override fun verbose(tag: String?, msg: () -> String) {
        logger.v(tag, msg)
    }

    override fun debug(tag: String?, msg: () -> String) {
        logger.d(tag, msg)
    }

    override fun info(tag: String?, msg: () -> String,) {
        logger.i(tag, msg)
    }

    override fun warn(t: Throwable?, tag: String?, msg: () -> String) {
        logger.w(t, tag, msg)
    }

    override fun error(t: Throwable?, tag: String?, msg: () -> String) {
        logger.e(t, tag, msg)
    }

    override fun wtf(t: Throwable?, tag: String?, msg: () -> String) {
        logger.e(t, tag, msg)
    }
}

inline fun <reified T> org.koin.core.scope.Scope.logger(): ScanBridgeLogger {
    val tag = T::class.simpleName ?: "Unknown"
    return get { org.koin.core.parameter.parametersOf(tag) }
}

inline fun <reified T> logger() = lazy {
    val koin = getKoin()
    koin.get<ScanBridgeLogger> {
        org.koin.core.parameter.parametersOf(T::class.simpleName ?: "Unknown")
    }
}
