package io.github.chrisimx.scanbridge.model

sealed interface IpAddress {
    val bytes: ByteArray

    val text: String

    val urlHost: String
        get() = text

    class V4(
        override val bytes: ByteArray,
    ) : IpAddress {
        init {
            require(bytes.size == 4) {
                "A IPv4 address needs to be 4 bytes long"
            }
        }

        override val text: String
            get() = bytes.joinToString(".") { it.toUByte().toString() }
    }

    class V6(
        override val bytes: ByteArray,
        val scopeId: String? = null
    ) : IpAddress {
        init {
            require(bytes.size == 16) {
                "A IPv6 address needs to be 16 bytes long"
            }
        }

        val simpleIpString = bytes.asIterable()
            .chunked(2)
            .joinToString(":") { chunk ->
                val high = chunk[0].toUByte().toInt()
                val low = chunk[1].toUByte().toInt()
                ((high shl 8) or low).toString(16)
            }

        override val text: String
            get() {

                return simpleIpString + (scopeId?.let { "%$scopeId" } ?: "")
            }

        override val urlHost: String
            get() = "[$simpleIpString]" // IPv6 needs brackets in URLs
    }
}
