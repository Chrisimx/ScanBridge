package io.github.chrisimx.scanbridge.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import io.github.chrisimx.scanbridge.proto.LastRoute
import io.github.chrisimx.scanbridge.proto.lastRoute
import java.io.InputStream
import java.io.OutputStream

object LastRouteStoreSerializer : Serializer<LastRoute> {
    override val defaultValue: LastRoute = lastRoute {}

    override suspend fun readFrom(input: InputStream): LastRoute {
        try {
            return LastRoute.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: LastRoute, output: OutputStream) = t.writeTo(output)
}
