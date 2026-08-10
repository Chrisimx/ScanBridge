package io.github.chrisimx.scanbridge.model

import io.ktor.http.Url

sealed class UrlValidationResult {
    data class NoError(val url: Url) : UrlValidationResult()
    data object Empty : UrlValidationResult()
    data object InvalidUrl : UrlValidationResult()
}
