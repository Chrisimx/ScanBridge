package io.github.chrisimx.scanbridge.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import io.github.chrisimx.scanbridge.proto.ShownMessages
import java.io.InputStream
import java.io.OutputStream

object ShownMessagesSerializer : Serializer<ShownMessages> {
    override val defaultValue: ShownMessages = ShownMessages.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): ShownMessages {
        try {
            return ShownMessages.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: ShownMessages, output: OutputStream) = t.writeTo(output)
}
