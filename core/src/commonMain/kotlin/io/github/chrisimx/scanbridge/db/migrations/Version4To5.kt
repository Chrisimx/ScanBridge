import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.anyscancompat.toCommonAbstraction
import io.github.chrisimx.scanbridge.ScanSettingsJson
import io.github.chrisimx.scanbridge.model.ScanSettingsEnterableDataV0

val MIGRATION_4_5 = object : Migration(4, 5) {

    override fun migrate(connection: SQLiteConnection) {
        migrateCustomScanners(connection)

        migrateScannedPages(connection)

        migrateSessions(connection)
    }

    // Entity: CustomScanner
    // + protocolIdentifier column: String = eSCL
    private fun migrateCustomScanners(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE customscanners ADD COLUMN protocolIdentifier TEXT NOT NULL DEFAULT 'eSCL'")
    }

    // Entity: ScannedPage
    // C originalScanSettings: type change: eSCL ScanSettings -> CommonScanSettings
    // + outputName column: String? = null
    private fun migrateScannedPages(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE scannedpages ADD COLUMN outputName TEXT")

        val scannedPagesQuery = connection.prepare("SELECT scanId, originalScanSettings FROM scannedpages")
        val scannedPagesUpdate = connection.prepare("UPDATE scannedpages SET originalScanSettings = ? WHERE scanId = ?")

        val json = ScanSettingsJson.json

        scannedPagesUpdate.use {
            scannedPagesQuery.use {
                while (scannedPagesQuery.step()) {
                    check(scannedPagesQuery.getColumnName(0) == "scanId")
                    check(scannedPagesQuery.getColumnName(1) == "originalScanSettings")

                    val scanId = scannedPagesQuery.getText(0)

                    val oldOriginalScanSettingsString = scannedPagesQuery.getText(1)
                    val deserializedOldOriginalScanSettings = json
                        .decodeFromString<ScanSettings>(oldOriginalScanSettingsString)

                    val newOriginalScanSettings = deserializedOldOriginalScanSettings.toCommonAbstraction()
                    val newOriginalScanSettingsString = json.encodeToString(newOriginalScanSettings)

                    scannedPagesUpdate.bindText(1, newOriginalScanSettingsString)
                    scannedPagesUpdate.bindText(2, scanId)
                    scannedPagesUpdate.step()
                    scannedPagesUpdate.clearBindings()
                }
            }
        }
    }

    // Entity: Session
    // C currentScanSettings: type change: eSCL ScanSettings? -> CommonScanSettings?
    // C currentSettingsUIData: type change: ScanSettingsEnterableDataV0? -> ScanSettingsEnterableDataV1?
    private fun migrateSessions(connection: SQLiteConnection) {
        val json = ScanSettingsJson.json

        val sessionsQuery = connection.prepare("SELECT sessionId, currentScanSettings, currentSettingsUIData FROM sessions")
        val sessionsUpdate = connection.prepare(
            "UPDATE sessions SET currentScanSettings = ?, currentSettingsUIData = ? WHERE sessionId = ?"
        )

        sessionsUpdate.use {
            sessionsQuery.use {
                while (sessionsQuery.step()) {
                    check(sessionsQuery.getColumnName(0) == "sessionId")
                    check(sessionsQuery.getColumnName(1) == "currentScanSettings")
                    check(sessionsQuery.getColumnName(2) == "currentSettingsUIData")

                    val sessionId = sessionsQuery.getText(0)

                    val oldCurrentScanSettingsString = if (!sessionsQuery.isNull(1)) {
                        sessionsQuery.getText(1)
                    } else {
                        null
                    }

                    val oldCurrentSettingsUIDataString = if (!sessionsQuery.isNull(2)) {
                        sessionsQuery.getText(2)
                    } else {
                        null
                    }

                    val oldCurrentScanSettings = oldCurrentScanSettingsString?.let {
                        json
                            .decodeFromString<ScanSettings?>(it)
                    }

                    val oldCurrentSettingsUIData = oldCurrentSettingsUIDataString?.let {
                        json
                            .decodeFromString<ScanSettingsEnterableDataV0?>(it)
                    }

                    val newCurrentScanSettings = oldCurrentScanSettings?.toCommonAbstraction()

                    val newCurrentSettingsUIData = oldCurrentSettingsUIData?.toV1()

                    val newCurrentScanSettingsString = json.encodeToString(newCurrentScanSettings)
                    val newCurrentSettingsUIDataString = json.encodeToString(newCurrentSettingsUIData)

                    sessionsUpdate.bindText(1, newCurrentScanSettingsString)
                    sessionsUpdate.bindText(2, newCurrentSettingsUIDataString)
                    sessionsUpdate.bindText(3, sessionId)
                    sessionsUpdate.step()
                    sessionsUpdate.clearBindings()
                }
            }
        }
    }
}
