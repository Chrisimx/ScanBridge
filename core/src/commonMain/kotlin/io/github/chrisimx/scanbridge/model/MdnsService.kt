package io.github.chrisimx.scanbridge.model

data class MdnsService(
    val serviceName: String,
    val serviceType: String,
    val port: Int,
    val addresses: List<IpAddress>,
    val txtAttributes: Map<String, ByteArray>
)
