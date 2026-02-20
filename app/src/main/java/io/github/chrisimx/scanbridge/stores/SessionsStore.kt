package io.github.chrisimx.scanbridge.stores

import android.content.Context
import io.github.chrisimx.esclkt.Inches
import io.github.chrisimx.esclkt.LengthUnit
import io.github.chrisimx.esclkt.Millimeters
import io.github.chrisimx.esclkt.Points
import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.esclkt.ThreeHundredthsOfInch
import io.github.chrisimx.scanbridge.data.model.Session
import io.github.chrisimx.scanbridge.data.model.Session.Companion.fromString
import java.io.File
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import timber.log.Timber

object SessionsStore {

    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            polymorphic(LengthUnit::class) {
                subclass(Inches::class)
                subclass(Millimeters::class)
                subclass(ThreeHundredthsOfInch::class)
                subclass(Points::class)
            }
        }
        classDiscriminator = "type"
        prettyPrint = false
    }

    fun loadSession(application: Context, sessionID: String, caps: ScannerCapabilities?): Result<Session?> {
        Timber.d("Loading session $sessionID")

        val path = application.applicationInfo.dataDir + "/files/" + sessionID + ".session"
        val file = File(path)

        if (!file.exists()) {
            Timber.d("Could not find session file at $path")
            return Result.success(null)
        }

        val sessionFileString = file.readText()

        return fromString(sessionFileString, json, caps)
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun saveSession(session: Session, application: Context, sessionID: String): String {
        Timber.d("Saving session $sessionID with $session")

        val path = application.applicationInfo.dataDir + "/files/" + sessionID + ".session"
        val file = File(path)

        file.outputStream().use {
            json.encodeToStream(session, it)
        }

        return path
    }
}
