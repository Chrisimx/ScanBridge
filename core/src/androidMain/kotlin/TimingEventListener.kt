import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.Protocol

class TimingEventListener : EventListener() {
    private val startNanos = System.nanoTime()
    private var callId: String? = null

    private fun log(event: String) {
        val ms = (System.nanoTime() - startNanos) / 1_000_000
        println("HTTP timing: ${ms}ms - [Call: $callId] $event")
    }

    override fun callStart(call: Call) {
        callId = call.request().url.toString()
        log("callStart")
    }

    override fun dnsStart(call: Call, domainName: String) {
        log("dnsStart $domainName")
    }

    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
        log("dnsEnd $inetAddressList")
    }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        log("connectStart $inetSocketAddress")
    }

    override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?) {
        log("connectEnd $protocol")
    }

    override fun secureConnectStart(call: Call) {
        log("tlsStart")
    }

    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        log("tlsEnd")
    }

    override fun requestHeadersStart(call: Call) {
        log("requestHeadersStart")
    }

    override fun responseHeadersStart(call: Call) {
        log("responseHeadersStart")
    }

    override fun responseBodyEnd(call: Call, byteCount: Long) {
        log("responseBodyEnd ${byteCount} bytes")
    }

    override fun callFailed(call: Call, ioe: IOException) {
        log("callFailed ${ioe::class.simpleName}: ${ioe.message}")
    }

    override fun callEnd(call: Call) {
        log("callEnd")
    }
}
