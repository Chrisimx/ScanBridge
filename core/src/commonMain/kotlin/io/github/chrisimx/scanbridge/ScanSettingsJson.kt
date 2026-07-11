package io.github.chrisimx.scanbridge

import io.github.chrisimx.anyscan.Inches
import io.github.chrisimx.anyscan.LengthUnit
import io.github.chrisimx.anyscan.Millimeters
import io.github.chrisimx.anyscan.Points
import io.github.chrisimx.anyscan.ThreeHundredthsOfInch
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
