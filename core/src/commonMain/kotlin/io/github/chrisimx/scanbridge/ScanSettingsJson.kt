package io.github.chrisimx.scanbridge

import io.github.chrisimx.csa.Inches
import io.github.chrisimx.csa.LengthUnit
import io.github.chrisimx.csa.Millimeters
import io.github.chrisimx.csa.Points
import io.github.chrisimx.csa.ThreeHundredthsOfInch
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
