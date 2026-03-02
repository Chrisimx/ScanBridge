package io.github.chrisimx.scanbridge.util

import io.github.chrisimx.esclkt.Inches
import io.github.chrisimx.esclkt.LengthUnit
import io.github.chrisimx.esclkt.Millimeters
import io.github.chrisimx.esclkt.Points
import io.github.chrisimx.esclkt.ThreeHundredthsOfInch
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

object ScanSettingsJson {
    val json = Json {
        ignoreUnknownKeys = false
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
}
