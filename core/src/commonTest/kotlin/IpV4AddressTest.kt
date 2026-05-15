import io.github.chrisimx.scanbridge.model.IpAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.writeUInt

class IpV4AddressTest {

    @Test
    fun `test V4 text representation is correct for a subset of the possible IPv4 addresses`() {
        val buffer = Buffer()
        for (ipAddressBytes in 0u..461804u) {
            buffer.writeUInt(ipAddressBytes)

            val fuzzByteArray = buffer.readByteArray()

            val constructedIp = IpAddress.V4(fuzzByteArray)

            val text = constructedIp.text

            assertEquals(3, text.count { it == '.' }, "Invalid IP text: $text")
            assertEquals(
                text,
                constructedIp.urlHost,
                "Url representation is not the same as text representation, but it should always be for IPv4"
            )
        }
    }

    @Test
    fun `test V4 with valid 4-byte input returns correct text representation`() {
        val bytes = byteArrayOf(192.toByte(), 168.toByte(), 0.toByte(), 1.toByte())

        val address = IpAddress.V4(bytes)

        assertEquals("192.168.0.1", address.text)
        assertEquals(address.urlHost, address.text)
    }

    @Test
    fun `test V4 with all-zero bytes returns correct text representation`() {
        val bytes = ByteArray(4) { 0u.toByte() }

        val address = IpAddress.V4(bytes)

        assertEquals("0.0.0.0", address.text)
        assertEquals(address.urlHost, address.text)
    }

    @Test
    fun `test V4 with maximum values in bytes returns correct text representation`() {
        val bytes = ByteArray(4) { 255u.toByte() }

        val address = IpAddress.V4(bytes)

        assertEquals("255.255.255.255", address.text)
        assertEquals(address.urlHost, address.text)
    }

    @Test
    fun `test V4 initialization with less than 4 bytes throws exception`() {
        val bytes = byteArrayOf(192.toByte(), 168.toByte(), 1.toByte())

        val exception = assertFailsWith<IllegalArgumentException> {
            IpAddress.V4(bytes)
        }
        assertEquals("A IPv4 address needs to be 4 bytes long", exception.message)
    }

    @Test
    fun `test V4 initialization with more than 4 bytes throws exception`() {
        val bytes = byteArrayOf(192.toByte(), 168.toByte(), 0.toByte(), 1.toByte(), 10.toByte())

        val exception = assertFailsWith<IllegalArgumentException> {
            IpAddress.V4(bytes)
        }
        assertEquals("A IPv4 address needs to be 4 bytes long", exception.message)
    }

    @Test
    fun `test urlHost property returns correct value as text`() {
        val bytes = byteArrayOf(127.toByte(), 0.toByte(), 0.toByte(), 1.toByte())

        val address = IpAddress.V4(bytes)

        assertEquals("127.0.0.1", address.urlHost)
    }
}
