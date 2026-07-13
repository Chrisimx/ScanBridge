import io.github.chrisimx.anyscan.Area
import io.github.chrisimx.anyscan.MutableScannerCapabilityMap
import io.github.chrisimx.anyscan.ScanSettingParam
import io.github.chrisimx.anyscan.ScannerCapabilityMap
import io.github.chrisimx.anyscan.ScannerConcept
import io.github.chrisimx.anyscan.inches
import io.github.chrisimx.anyscan.millimeters
import io.github.chrisimx.scanbridge.ScanSettingsJson
import kotlin.test.Test

class ScannerCapabilityMapJsonSerializationTest {
    @Test
    fun serialization() {
        val caps = MutableScannerCapabilityMap()
        caps += ScanSettingParam.ScanSettingRegionParam(
            ScannerConcept.ScanRegion,
            Area(200.millimeters(), 200.inches()),
            Area(100.millimeters(), 100.inches()),
            Area(300.millimeters(), 300.inches())
        )

        val json = ScanSettingsJson.json
        val capsImmutable = ScannerCapabilityMap.fromMutable(caps)
        println(json.encodeToString(capsImmutable))
    }
}
