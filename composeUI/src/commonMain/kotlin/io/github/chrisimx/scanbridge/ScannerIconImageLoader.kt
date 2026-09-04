package io.github.chrisimx.scanbridge

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.github.chrisimx.scanbridge.model.HttpClientConfig
import io.github.chrisimx.scanbridge.ports.HttpClientFactory
import io.ktor.client.HttpClient

fun createScannerIconImageLoader(factory: HttpClientFactory, context: PlatformContext): ImageLoader {
    val ktorClient: HttpClient = factory.create(
        HttpClientConfig(
            disableCertValidation = true,
            debugLogging = false,
            requestTimeoutInSeconds = 2u,
            connectTimeoutInSeconds = 2u,
            socketTimeoutInSeconds = 2u
        )
    )
    return ImageLoader.Builder(context)
        .components {
            add(
                KtorNetworkFetcherFactory(
                    httpClient = ktorClient
                )
            )
        }
        .build()
}

