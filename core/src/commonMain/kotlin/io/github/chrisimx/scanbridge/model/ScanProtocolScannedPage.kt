package io.github.chrisimx.scanbridge.model

data class ScanProtocolScannedPage(
    val contentType: String,
    val data: ByteArray,
) {
    override fun toString(): String =
        "ScannedPage(contentType='$contentType', data.size=${data.size})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScanProtocolScannedPage) return false

        if (contentType != other.contentType) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contentType.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
