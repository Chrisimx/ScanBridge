package io.github.chrisimx.scanbridge.paperformat

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val KOIN_MODULE_PAPER_FORMAT = module {
    single<DefaultPaperFormatProvider>() bind PaperFormatProvider::class
}
