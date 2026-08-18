package io.github.chrisimx.scanbridge.logging

import com.diamondedge.logging.KmLog
import com.diamondedge.logging.logging

class KmLogScanBridgeLogger(tag: String) : ScanBridgeLogger {
    private val logger: KmLog = logging(tag)

    override fun verbose(tag: String?, msg: () -> String) {
        logger.v(tag, msg)
    }

    override fun debug(tag: String?, msg: () -> String) {
        logger.d(tag, msg)
    }

    override fun info(tag: String?, msg: () -> String) {
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
