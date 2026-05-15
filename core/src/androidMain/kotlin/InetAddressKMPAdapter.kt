import io.github.chrisimx.scanbridge.model.IpAddress
import java.net.InetAddress

fun InetAddress.toMultiplatformIpAddress(): IpAddress {
    val raw = address

    return when (raw.size) {
        4 -> IpAddress.V4(
            bytes = raw,
        )

        16 -> IpAddress.V6(
            bytes = raw,
            scopeId = hostAddress!!.substringAfter('%', missingDelimiterValue = "")
                .takeIf { it.isNotBlank() }
        )

        else -> error("Unsupported InetAddress size: ${raw.size}")
    }
}
