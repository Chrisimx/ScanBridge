import io.github.chrisimx.localization.JvmLocaleProvider
import io.github.chrisimx.localization.JvmNumberFormatter
import io.github.chrisimx.scanbridge.localization.LocaleProvider
import io.github.chrisimx.scanbridge.localization.NumberFormatter
import io.github.chrisimx.scanbridge.ports.HttpClientFactory
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val KOIN_MODULE_JVM_AND_ANDROID_PLATFORM = module {
    single<JvmHttpClientFactory>() bind HttpClientFactory::class
    single<JvmLocaleProvider>() bind LocaleProvider::class
    single<JvmNumberFormatter>() bind NumberFormatter::class
}
