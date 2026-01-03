package io.github.chrisimx.scanbridge

import com.google.android.vending.licensing.Obfuscator

class PlainTextObfuscator : Obfuscator {
    override fun obfuscate(original: String?, key: String?): String? = original

    override fun unobfuscate(obfuscated: String?, key: String?): String? = obfuscated
}
