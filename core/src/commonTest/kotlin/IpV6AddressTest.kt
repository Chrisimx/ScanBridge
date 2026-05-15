import io.github.chrisimx.scanbridge.model.IpAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IpV6AddressTest {

    @Test
    fun `test V6 text representation with valid 16-byte IPv6 address`() {
        val bytes = byteArrayOf(
            0x20.toByte(), 0x01.toByte(), 0x0d.toByte(), 0xb8.toByte(),
            0x85.toByte(), 0xa3.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x8a.toByte(), 0x2e.toByte(), 0x03.toByte(), 0x70.toByte(),
            0x73.toByte(), 0x34.toByte(), 0x00.toByte(), 0x00.toByte()
        )

        val address = IpAddress.V6(bytes)

        assertEquals(
            "2001:db8:85a3:0:8a2e:370:7334:0",
            address.text
        )
        assertEquals("[2001:db8:85a3:0:8a2e:370:7334:0]", address.urlHost)
    }

    @Test
    fun `test V6 with all-zero bytes returns compressed text representation`() {
        val bytes = ByteArray(16) { 0.toByte() }

        val address = IpAddress.V6(bytes)

        assertEquals("0:0:0:0:0:0:0:0", address.text)
        assertEquals("[0:0:0:0:0:0:0:0]", address.urlHost)
    }

    @Test
    fun `test V6 with maximum values in bytes returns uncompressed text representation`() {
        val bytes = ByteArray(16) { 0xFF.toByte() }

        val address = IpAddress.V6(bytes)

        assertEquals("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", address.text)
        assertEquals("[ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff]", address.urlHost)
    }

    @Test
    fun `test V6 initialization with less than 16 bytes throws exception`() {
        val bytes = byteArrayOf(
            0x20.toByte(), 0x01.toByte(), 0x0d.toByte(), 0xb8.toByte(),
            0x85.toByte(), 0xa3.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x8a.toByte(), 0x2e.toByte(), 0x03.toByte(), 0x70.toByte()
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            IpAddress.V6(bytes)
        }
        assertEquals("A IPv6 address needs to be 16 bytes long", exception.message)
    }

    @Test
    fun `test V6 initialization with more than 16 bytes throws exception`() {
        val bytes = byteArrayOf(
            0x20.toByte(), 0x01.toByte(), 0x0d.toByte(), 0xb8.toByte(),
            0x85.toByte(), 0xa3.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x8a.toByte(), 0x2e.toByte(), 0x03.toByte(), 0x70.toByte(),
            0x73.toByte(), 0x34.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte()
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            IpAddress.V6(bytes)
        }
        assertEquals("A IPv6 address needs to be 16 bytes long", exception.message)
    }

    @Test
    fun `test V6 text with scopeId returns correct URL host`() {
        val bytes = byteArrayOf(
            0xfe.toByte(), 0x80.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x02.toByte(), 0x1c.toByte(), 0xff.toByte(), 0xfe.toByte(),
            0x6c.toByte(), 0x60.toByte(), 0xc2.toByte(), 0x63.toByte()
        )
        val scopeId = "eth0"

        val address = IpAddress.V6(bytes, scopeId)

        assertEquals("fe80:0:0:0:21c:fffe:6c60:c263%eth0", address.text)
        assertEquals("[fe80:0:0:0:21c:fffe:6c60:c263]", address.urlHost)
    }
}
