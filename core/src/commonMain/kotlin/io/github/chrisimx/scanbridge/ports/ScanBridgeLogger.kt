package io.github.chrisimx.scanbridge.ports

interface ScanBridgeLogger {
    /** Log a verbose message */
    fun verbose(tag: String? = null, msg: () -> String)

    /** Log a debug message */
    fun debug(tag: String? = null, msg: () -> String)

    /** Log an info message */
    fun info(tag: String? = null, msg: () -> String)

    /** Log a warning message with optional throwable */
    fun warn(t: Throwable? = null, tag: String? = null, msg: () -> String)

    /** Log an error message with optional throwable */
    fun error(t: Throwable? = null, tag: String? = null, msg: () -> String)

    /** Log a critical/fatal message with optional throwable */
    fun wtf(t: Throwable? = null, tag: String? = null, msg: () -> String)
}
