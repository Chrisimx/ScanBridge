package io.github.chrisimx.scanbridge.logs

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.buffer
import okio.sink
import timber.log.Timber

class DebugInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        chain.request().let {
            Timber.tag("DebugInterceptor")
                .d(
                    "Request: itself $it URL: ${it.url} method: ${it.method} headers: ${it.headers} body: ${it.body} cacheControl: ${it.cacheControl} isHttps: ${it.isHttps} tag: ${it.tag()}"
                )

            it.body?.let { body ->
                Timber.tag("DebugInterceptor").d("Request body: ${body.contentLength()} bytes")
                if (!body.isOneShot()) {
                    Timber.tag("DebugInterceptor").d("Request body is not one-shot")
                    ByteArrayOutputStream().use {
                        val sink = it.sink().buffer()
                        body.writeTo(sink)
                        sink.flush()
                        sink.close()
                        Timber.tag("DebugInterceptor").d("Request body: ${it.toString(StandardCharsets.UTF_8)}")
                    }
                } else {
                    Timber.tag("DebugInterceptor").d("Request body is one-shot")
                }
            }
        }

        return chain.proceed(chain.request()).let {
            Timber.tag("DebugInterceptor")
                .d(
                    "Response: itself: $it, code: ${it.code} headers: ${it.headers} handshake: ${it.handshake} isRedirect: ${it.isRedirect} message: ${it.message} isSuccessful: ${it.isSuccessful} body: ${it.body} cacheControl: ${it.cacheControl} cacheResponse: ${it.cacheResponse} networkResponse: ${it.networkResponse} prior response: ${it.priorResponse} protocol: ${it.protocol} receivedResponseAt: ${it.receivedResponseAtMillis} sentRequestAtMillis: ${it.sentRequestAtMillis} request_url: ${it.request.url} request_method: ${it.request.method} request_headers: ${it.request.headers} request_body: ${it.request.body}"
                )

            val responseString = it.body?.string()
            val contentType = it.body?.contentType()

            Timber.tag("DebugInterceptor")
                .d("Response body: length: ${it.body?.contentLength()} content-type: ${it.body?.contentType()} - String: $responseString")

            if (responseString == null || responseString.isEmpty()) {
                return it
            }

            val wrappedBody = responseString.toResponseBody(contentType)

            it.newBuilder().body(wrappedBody).build()
        }
    }
}
