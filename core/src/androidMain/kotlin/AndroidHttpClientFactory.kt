import io.github.chrisimx.scanbridge.model.HttpClientConfig
import io.github.chrisimx.scanbridge.ports.HttpClientFactory
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging

class AndroidHttpClientFactory(loggerFactory: ScanBridgeLoggerFactory) : HttpClientFactory {
    val httpClientLogger = loggerFactory.withTag("AndroidHttpClient")

    override fun create(config: HttpClientConfig): HttpClient = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = config.requestTimeoutInSeconds.toLong() * 1000
            connectTimeoutMillis = config.connectTimeoutInSeconds.toLong() * 1000
            socketTimeoutMillis = config.socketTimeoutInSeconds.toLong() * 1000
        }
        if (config.debugLogging) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        httpClientLogger.debug { message }
                    }
                }
            }
        }
        if (config.disableCertValidation) {
            engine {
                config {
                    val (socketFactory, trustManager) = getTrustAllTM()
                    sslSocketFactory(socketFactory, trustManager)
                    hostnameVerifier { _, _ -> true }
                }
            }
        }
    }
}
