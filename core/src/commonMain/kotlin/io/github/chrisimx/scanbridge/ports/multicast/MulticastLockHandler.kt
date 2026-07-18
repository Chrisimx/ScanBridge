package io.github.chrisimx.scanbridge.ports.multicast

interface MulticastLockHandler {
    fun acquire()
    fun release()
}
