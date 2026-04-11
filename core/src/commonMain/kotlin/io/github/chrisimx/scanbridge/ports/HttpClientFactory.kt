package io.github.chrisimx.scanbridge.ports

import io.github.chrisimx.scanbridge.model.HttpClientConfig
import io.ktor.client.HttpClient

interface HttpClientFactory {
    fun create(config: HttpClientConfig): HttpClient
}
