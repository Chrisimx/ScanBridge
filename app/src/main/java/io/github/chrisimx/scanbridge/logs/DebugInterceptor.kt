package io.github.chrisimx.scanbridge.logs

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import okhttp3.Interceptor
import okhttp3.Response
import okio.buffer
import okio.sink
import timber.log.Timber

class DebugInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        chain.request().let {
            Timber.tag("DebugInterceptor").d("Request: $it")

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
                }
            }
        }

        return chain.proceed(chain.request()).let {
            Timber.tag("DebugInterceptor").d("Response: $it")

            if (!it.isSuccessful) {
                Timber.tag("DebugInterceptor").d("Response body: ${it.body?.string()}")
            }

            it
        }
    }
}
